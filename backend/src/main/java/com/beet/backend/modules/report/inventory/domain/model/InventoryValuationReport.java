package com.beet.backend.modules.report.inventory.domain.model;

import com.beet.backend.modules.report.core.domain.model.ReportResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InventoryValuationReport(
        BigDecimal knownValue,
        int valuedIngredientCount,
        int missingCostIngredientCount,
        List<MissingCostIngredient> missingCostIngredients) implements ReportResult {

    public record MissingCostIngredient(
            UUID restaurantId,
            String restaurantName,
            UUID ingredientId,
            String ingredientName,
            BigDecimal currentStock) {
    }
}
