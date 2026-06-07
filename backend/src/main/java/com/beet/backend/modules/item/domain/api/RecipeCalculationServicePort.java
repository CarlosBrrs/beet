package com.beet.backend.modules.item.domain.api;

import com.beet.backend.modules.item.domain.model.RecipeCalculationResult;

import java.util.UUID;

public interface RecipeCalculationServicePort {

    RecipeCalculationResult calculate(UUID restaurantId, UUID itemId);

    RecipeCalculationResult recalculate(UUID restaurantId, UUID itemId, UUID userId);

    void recalculateDependentsForIngredient(UUID masterIngredientId, UUID userId);
}
