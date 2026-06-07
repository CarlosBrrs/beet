package com.beet.backend.modules.item.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeIngredientRequirement(
        UUID masterIngredientId,
        String ingredientName,
        UUID baseUnitId,
        String baseUnitAbbreviation,
        BigDecimal quantityBase,
        BigDecimal unitCost) {
}
