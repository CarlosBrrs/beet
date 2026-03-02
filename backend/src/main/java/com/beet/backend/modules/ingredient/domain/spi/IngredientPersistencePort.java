package com.beet.backend.modules.ingredient.domain.spi;

import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import java.util.UUID;

public interface IngredientPersistencePort {

    MasterIngredientDomain saveMasterIngredient(MasterIngredientDomain ingredient);

    SupplierItemDomain saveSupplierItem(SupplierItemDomain supplierItem);

    void updateActiveSupplierItem(UUID masterIngredientId, UUID supplierItemId);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    boolean existsByIdAndOwnerId(UUID ingredientId, UUID ownerId);
}
