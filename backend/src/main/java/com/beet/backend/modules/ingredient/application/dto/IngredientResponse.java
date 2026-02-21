package com.beet.backend.modules.ingredient.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record IngredientResponse(
        UUID id,
        String name,
        UUID baseUnitId,
        UUID activeSupplierItemId,
        SupplierItemInfo supplierItem) {
    public record SupplierItemInfo(
            UUID id,
            UUID supplierId,
            String brandName,
            String purchaseUnitName,
            BigDecimal conversionFactor,
            BigDecimal lastCostBase) {
    }
}
