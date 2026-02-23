package com.beet.backend.modules.ingredient.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Enriched single-ingredient response for the detail / edit panel.
 * Includes supplier chain info from the active supplier item.
 */
public record IngredientDetailResponse(
        UUID id,
        String name,
        UUID baseUnitId,
        String unitName, // units.name e.g. "Kilogram"
        String unitAbbreviation, // units.abbreviation e.g. "kg"
        BigDecimal costPerBaseUnit, // null if no active supplier linked yet
        ActiveSupplierInfo activeSupplier // null if no supplier linked yet
) {
    public record ActiveSupplierInfo(
            UUID supplierId,
            String supplierName,
            UUID supplierItemId,
            String brandName,
            String purchaseUnitName,
            BigDecimal conversionFactor,
            BigDecimal lastCostBase) {
    }
}
