package com.beet.backend.modules.item.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a PREPARATION (Capa 1).
 * Preparations are always inventory-tracked and have no sale_price.
 */
public record CreatePreparationRequest(

        @NotBlank(message = "Name is required") String name,

        String description,

        @NotNull(message = "Yield quantity is required") @Positive(message = "Yield quantity must be positive") @Digits(integer = 10, fraction = 4) BigDecimal yieldQty,

        @NotNull(message = "Yield unit is required") UUID yieldUnitId,

        @NotNull(message = "Recipe lines are required") @Valid List<RecipeLineRequest> lines) {
}
