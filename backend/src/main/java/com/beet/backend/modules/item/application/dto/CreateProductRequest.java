package com.beet.backend.modules.item.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a SALEABLE_PRODUCT (Capa 2).
 * Supports both tracked (with recipe) and flat (no recipe) products.
 *
 * Rules:
 * - isInventoryTracked=true + lines → system calculates theoreticalCost
 * - isInventoryTracked=false + userCost → userCost is used as theoreticalCost
 */
public record CreateProductRequest(

        @NotBlank(message = "Name is required") String name,

        String description,

        @NotNull(message = "Sale price is required") @Positive(message = "Sale price must be positive") @Digits(integer = 12, fraction = 2) BigDecimal salePrice,

        @NotNull(message = "isInventoryTracked is required") Boolean isInventoryTracked,

        // Only for flat products (isInventoryTracked=false)
        @PositiveOrZero(message = "User defined cost must be >= 0") @Digits(integer = 12, fraction = 4) BigDecimal userDefinedCost,

        // Required when isInventoryTracked=true
        @Valid List<RecipeLineRequest> lines,

        // Yield — relevant for tracked products; defaults to 1 pc
        @Positive(message = "Yield quantity must be positive") BigDecimal yieldQty,
        UUID yieldUnitId) {
}
