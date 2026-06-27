package com.beet.backend.modules.item.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecipeCalculationResult(
        BigDecimal batchCost,
        BigDecimal physicalYieldInBase,
        UUID physicalBaseUnitId,
        String physicalBaseUnitAbbreviation,
        BigDecimal costPerPhysicalBaseUnit,
        BigDecimal costPerSellableUnit,
        List<RecipeIngredientRequirement> batchRequirements,
        List<RecipeIngredientRequirement> ingredientRequirementsPerSellableUnit,
        List<String> missingCostIngredients) {

    public boolean costComplete() {
        return missingCostIngredients == null || missingCostIngredients.isEmpty();
    }
}
