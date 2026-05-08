package com.beet.backend.modules.template.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateTemplateRequest(

        @NotBlank(message = "Name is required") String name,

        String description,

        @NotNull(message = "Base price is required") @PositiveOrZero(message = "Base price must be >= 0") @Digits(integer = 12, fraction = 2) BigDecimal basePrice,

        @NotNull(message = "Slots are required") @Valid List<CreateSlotRequest> slots) {
}
