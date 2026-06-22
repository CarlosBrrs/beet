package com.beet.backend.modules.ingredient.domain.usecase;

import com.beet.backend.modules.ingredient.domain.api.IngredientServicePort;
import com.beet.backend.modules.ingredient.domain.exception.IngredientAlreadyExistsException;
import com.beet.backend.modules.ingredient.domain.exception.IngredientNotFoundException;
import com.beet.backend.modules.ingredient.domain.exception.UnitTypeMismatchException;
import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.ingredient.domain.spi.IngredientPersistencePort;
import com.beet.backend.modules.supplier.domain.api.SupplierServicePort;
import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import com.beet.backend.modules.unit.domain.api.UnitServicePort;
import com.beet.backend.modules.unit.domain.model.UnitDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        ingredient.setName(cleanRequired(ingredient.getName(), "Ingredient name is required"));
        if (persistencePort.existsByNameAndOwnerId(ingredient.getName(), ownerId)) {
            throw IngredientAlreadyExistsException.forName(ingredient.getName());
        }

        UnitDomain baseUnit = unitServicePort.findById(ingredient.getBaseUnitId());
        UnitDomain conversionUnit = unitServicePort.findById(conversionUnitId);

        validateUnits(baseUnit, conversionUnit, ingredient.getBaseUnitId(), conversionUnitId);

        BigDecimal finalFactor = userConversionFactor.multiply(conversionUnit.getFactorToBase());
        BigDecimal lastCostBase = totalPrice.divide(finalFactor, 6, RoundingMode.HALF_UP);

        SupplierDomain resolvedSupplier = supplierServicePort.findOrCreate(supplier, ownerId);

        ingredient.setOwnerId(ownerId);
        MasterIngredientDomain savedIngredient = persistencePort.saveMasterIngredient(ingredient);

        supplierItem.setMasterIngredientId(savedIngredient.getId());
        supplierItem.setSupplierId(resolvedSupplier.getId());
        supplierItem.setConversionFactor(finalFactor);
        supplierItem.setLastCostBase(lastCostBase);
        SupplierItemDomain savedItem = persistencePort.saveSupplierItem(supplierItem);
        supplierItem.setId(savedItem.getId());

        persistencePort.updateActiveSupplierItem(savedIngredient.getId(), savedItem.getId());

        savedIngredient.setActiveSupplierItemId(savedItem.getId());
        return savedIngredient;
    }

    @Override
    @Transactional
    public MasterIngredientDomain update(UUID ingredientId, MasterIngredientDomain ingredient, UUID ownerId) {
        MasterIngredientDomain existing = persistencePort.findByIdAndOwnerId(ingredientId, ownerId)
                .orElseThrow(() -> IngredientNotFoundException.forId(ingredientId));

        String cleanedName = cleanRequired(ingredient.getName(), "Ingredient name is required");
        if (persistencePort.existsByNameAndOwnerIdExcludingId(cleanedName, ownerId, ingredientId)) {
            throw IngredientAlreadyExistsException.forName(cleanedName);
        }

        if (ingredient.getBaseUnitId() == null) {
            throw new IllegalArgumentException("Base unit is required");
        }
        UnitDomain newBaseUnit = unitServicePort.findById(ingredient.getBaseUnitId());
        if (newBaseUnit == null) {
            throw UnitTypeMismatchException.unitNotFound(ingredient.getBaseUnitId());
        }

        if (!ingredient.getBaseUnitId().equals(existing.getBaseUnitId())
                && persistencePort.hasBaseUnitChangeBlockers(ingredientId)) {
            throw new IllegalArgumentException(
                    "Base unit cannot be changed after the ingredient has stock, recipe, supplier or order references.");
        }

        existing.setName(cleanedName);
        existing.setBaseUnitId(ingredient.getBaseUnitId());
        return persistencePort.saveMasterIngredient(existing);
    }

    @Override
    @Transactional
    public void delete(UUID ingredientId, UUID ownerId, UUID actorId) {
        persistencePort.findByIdAndOwnerId(ingredientId, ownerId)
                .orElseThrow(() -> IngredientNotFoundException.forId(ingredientId));
        if (persistencePort.hasDeleteBlockers(ingredientId)) {
            throw new IllegalArgumentException(
                    "Ingredient cannot be deleted while it has stock, recipes, orders or inventory history linked.");
        }
        persistencePort.softDelete(ingredientId, ownerId, actorId);
    }

    private void validateUnits(UnitDomain baseUnit, UnitDomain conversionUnit, UUID baseUnitId, UUID conversionUnitId) {
        if (baseUnit == null) {
            throw UnitTypeMismatchException.unitNotFound(baseUnitId);
        }
        if (conversionUnit == null) {
            throw UnitTypeMismatchException.unitNotFound(conversionUnitId);
        }
        if (baseUnit.getType() != conversionUnit.getType()) {
            throw UnitTypeMismatchException.between(baseUnit.getType().name(), conversionUnit.getType().name());
        }
    }

    private String cleanRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
