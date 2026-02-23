package com.beet.backend.modules.ingredient.domain.usecase;

import com.beet.backend.modules.ingredient.application.dto.IngredientDetailResponse;
import com.beet.backend.modules.ingredient.application.dto.IngredientListResponse;
import com.beet.backend.modules.ingredient.domain.api.IngredientServicePort;
import com.beet.backend.modules.ingredient.domain.exception.IngredientAlreadyExistsException;
import com.beet.backend.modules.ingredient.domain.exception.UnitTypeMismatchException;
import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.ingredient.domain.spi.IngredientPersistencePort;
import com.beet.backend.modules.supplier.domain.api.SupplierServicePort;
import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import com.beet.backend.modules.unit.domain.api.UnitServicePort;
import com.beet.backend.modules.unit.domain.model.UnitDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngredientUseCase implements IngredientServicePort {

    private final IngredientPersistencePort persistencePort;
    private final UnitServicePort unitServicePort;
    private final SupplierServicePort supplierServicePort;

    @Override
    @Transactional
    public MasterIngredientDomain create(
            MasterIngredientDomain ingredient,
            SupplierDomain supplier,
            SupplierItemDomain supplierItem,
            UUID conversionUnitId,
            BigDecimal userConversionFactor,
            BigDecimal totalPrice,
            UUID ownerId) {

        // 1. Validate: no duplicate ingredient name for this owner
        if (persistencePort.existsByNameAndOwnerId(ingredient.getName(), ownerId)) {
            throw IngredientAlreadyExistsException.forName(ingredient.getName());
        }

        // 2. Resolve both units via UnitServicePort
        UnitDomain baseUnit = unitServicePort.findById(ingredient.getBaseUnitId());
        UnitDomain conversionUnit = unitServicePort.findById(conversionUnitId);

        if (baseUnit == null) {
            throw UnitTypeMismatchException.unitNotFound(ingredient.getBaseUnitId());
        }
        if (conversionUnit == null) {
            throw UnitTypeMismatchException.unitNotFound(conversionUnitId);
        }

        // 3. Validate: both units must be the same type (MASS↔MASS, VOLUME↔VOLUME)
        if (baseUnit.getType() != conversionUnit.getType()) {
            throw UnitTypeMismatchException.between(
                    baseUnit.getType().name(), conversionUnit.getType().name());
        }

        // 4. Beet Math Engine — Calculate final factor
        // finalFactor = userConversionFactor × conversionUnit.factorToBase
        // e.g. 25 kg × 1000 (g/kg) = 25,000 g
        BigDecimal finalFactor = userConversionFactor.multiply(conversionUnit.getFactorToBase());

        // 5. Calculate cost per base unit
        // lastCostBase = totalPrice / finalFactor
        // e.g. 45,000 / 25,000 = 1.80
        BigDecimal lastCostBase = totalPrice.divide(finalFactor, 6, RoundingMode.HALF_UP);

        // 6. Resolve or create supplier via SupplierServicePort
        SupplierDomain resolvedSupplier = supplierServicePort.findOrCreate(supplier, ownerId);

        // 7. Persist master ingredient
        ingredient.setOwnerId(ownerId);
        MasterIngredientDomain savedIngredient = persistencePort.saveMasterIngredient(ingredient);

        // 8. Persist supplier item with computed values
        supplierItem.setMasterIngredientId(savedIngredient.getId());
        supplierItem.setSupplierId(resolvedSupplier.getId());
        supplierItem.setConversionFactor(finalFactor);
        supplierItem.setLastCostBase(lastCostBase);
        SupplierItemDomain savedItem = persistencePort.saveSupplierItem(supplierItem);
        supplierItem.setId(savedItem.getId());

        // 9. Activate: set the first supplier item as active
        persistencePort.updateActiveSupplierItem(savedIngredient.getId(), savedItem.getId());

        // 10. Return enriched domain
        savedIngredient.setActiveSupplierItemId(savedItem.getId());
        return savedIngredient;
    }

    @Override
    public PageResponse<IngredientListResponse> list(
            UUID ownerId, int page, int size,
            String search, String sortBy, boolean sortDesc, List<String> units) {
        return persistencePort.findAllByOwnerId(ownerId, page, size, search, sortBy, sortDesc, units);
    }

    @Override
    public Optional<IngredientDetailResponse> findById(UUID id, UUID ownerId) {
        return persistencePort.findDetailById(id, ownerId);
    }
}
