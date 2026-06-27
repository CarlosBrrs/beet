package com.beet.backend.modules.inventory.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ActivateIngredientRequest(
        @NotNull UUID masterIngredientId,
        @NotNull BigDecimal initialStock,
        BigDecimal minStock // optional, defaults to 0
) {
}
