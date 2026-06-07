package com.beet.backend.modules.item.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record IngredientCostSource(
        UUID ingredientId,
        String ingredientName,
        UUID baseUnitId,
        String baseUnitAbbreviation,
        BigDecimal unitCost) {
}
