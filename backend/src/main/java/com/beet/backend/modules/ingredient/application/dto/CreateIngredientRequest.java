package com.beet.backend.modules.ingredient.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateIngredientRequest(
        @Valid @NotNull MasterIngredientPayload masterIngredient,
        @Valid @NotNull SupplierPayload supplier,
        @Valid @NotNull SupplierItemPayload supplierItem) {

    public record MasterIngredientPayload(
            @NotBlank(message = "Ingredient name is required") String name,
            @NotNull(message = "Base unit is required") UUID baseUnitId) {
    }

    public record SupplierPayload(
            UUID id, // null = quick-add new supplier
            String name,
            UUID documentTypeId,
            String documentNumber) {
    }

    public record SupplierItemPayload(
            String brandName,
            @NotBlank(message = "Purchase unit name is required") String purchaseUnitName,
            @NotNull(message = "Conversion factor is required") @Positive BigDecimal conversionFactor,
            @NotNull(message = "Conversion unit is required") UUID conversionUnitId,
            @NotNull(message = "Total price is required") @Positive BigDecimal totalPrice) {
    }
}
