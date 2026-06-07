package com.beet.backend.modules.item.application.dto;

import com.beet.backend.modules.item.domain.model.RecipeLineSource;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeLineResponse(
        UUID id,
        RecipeLineSource source,
        UUID masterIngredientId,
        UUID childItemId,
        BigDecimal quantity,
        UUID unitId,
        String sourceName,
        String unitName,
        String unitAbbreviation,
        int sortOrder) {
}
