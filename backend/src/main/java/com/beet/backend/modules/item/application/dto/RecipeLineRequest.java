package com.beet.backend.modules.item.application.dto;

import com.beet.backend.modules.item.domain.model.RecipeLineSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single line in a composite item's recipe.
 * Can map to a master ingredient (source=INGREDIENT) or a nested preparation (source=PREPARATION).
 */
public record RecipeLineRequest(

        UUID masterIngredientId,
        UUID childItemId,
        RecipeLineSource source,

        @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be positive") BigDecimal quantity,

        @NotNull(message = "Unit is required") UUID unitId) {
}
