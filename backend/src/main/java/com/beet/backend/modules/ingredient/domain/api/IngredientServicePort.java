package com.beet.backend.modules.ingredient.domain.api;

import com.beet.backend.modules.ingredient.domain.model.MasterIngredientDomain;
import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.supplier.domain.model.SupplierDomain;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service port for ingredient operations.
 */
public interface IngredientServicePort {

        MasterIngredientDomain create(
                        MasterIngredientDomain ingredient,
                        SupplierDomain supplier,
                        SupplierItemDomain supplierItem,
                        UUID conversionUnitId,
                        BigDecimal userConversionFactor,
                        BigDecimal totalPrice,
                        UUID ownerId);

        MasterIngredientDomain update(UUID ingredientId, MasterIngredientDomain ingredient, UUID ownerId);

        void delete(UUID ingredientId, UUID ownerId, UUID actorId);
}
