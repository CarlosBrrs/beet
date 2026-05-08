package com.beet.backend.modules.template.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSlotOptionRequest(

        @NotNull(message = "Item ID is required for a slot option") UUID itemId,

        @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal surcharge,

        boolean isDefault,

        int sortOrder) {
}
