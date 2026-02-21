package com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.ingredient.domain.spi.IngredientPersistencePort;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.mapper.IngredientAggregateMapper;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.repository.MasterIngredientJdbcRepository;
import com.beet.backend.modules.ingredient.infrastructure.output.persistence.jdbc.repository.SupplierItemJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IngredientJdbcAdapter implements IngredientPersistencePort {

    private final MasterIngredientJdbcRepository ingredientRepository;
    private final SupplierItemJdbcRepository supplierItemRepository;
    private final IngredientAggregateMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public MasterIngredientDomain saveMasterIngredient(MasterIngredientDomain ingredient) {
        var saved = ingredientRepository.save(mapper.toAggregate(ingredient));
        return mapper.toDomain(saved);
    }

    @Override
    public SupplierItemDomain saveSupplierItem(SupplierItemDomain supplierItem) {
        var saved = supplierItemRepository.save(mapper.toAggregate(supplierItem));
        return mapper.toDomain(saved);
    }

    @Override
    public void updateActiveSupplierItem(UUID masterIngredientId, UUID supplierItemId) {
        // Direct UPDATE to avoid loading the full aggregate (avoids circular dependency
        // issues)
        jdbcTemplate.update(
                "UPDATE master_ingredients SET active_supplier_item_id = ? WHERE id = ?",
                supplierItemId, masterIngredientId);
    }

    @Override
    public boolean existsByNameAndOwnerId(String name, UUID ownerId) {
        return ingredientRepository.existsByNameAndOwnerId(name, ownerId);
    }
}
