package com.beet.backend.modules.ingredient.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateIngredientRequest(
        @NotBlank(message = "Ingredient name is required") String name,
        @NotNull(message = "Base unit is required") UUID baseUnitId) {
}
