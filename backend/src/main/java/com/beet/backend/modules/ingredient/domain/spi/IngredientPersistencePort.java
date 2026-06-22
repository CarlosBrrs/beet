package com.beet.backend.modules.ingredient.domain.spi;

import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import java.util.Optional;
import java.util.UUID;

public interface IngredientPersistencePort {

    MasterIngredientDomain saveMasterIngredient(MasterIngredientDomain ingredient);

    SupplierItemDomain saveSupplierItem(SupplierItemDomain supplierItem);

    void updateActiveSupplierItem(UUID masterIngredientId, UUID supplierItemId);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    boolean existsByNameAndOwnerIdExcludingId(String name, UUID ownerId, UUID excludedId);

    boolean existsByIdAndOwnerId(UUID ingredientId, UUID ownerId);

    Optional<MasterIngredientDomain> findByIdAndOwnerId(UUID ingredientId, UUID ownerId);

    boolean hasBaseUnitChangeBlockers(UUID ingredientId);

    boolean hasDeleteBlockers(UUID ingredientId);

    void softDelete(UUID ingredientId, UUID ownerId, UUID actorId);
}
