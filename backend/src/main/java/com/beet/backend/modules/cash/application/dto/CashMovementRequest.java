package com.beet.backend.modules.cash.application.dto;

import com.beet.backend.modules.cash.domain.model.CashMovementDirection;
import com.beet.backend.modules.cash.domain.model.CashMovementReason;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashMovementRequest(
        @NotNull CashMovementDirection direction,
        @NotNull CashMovementReason reason,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        String notes) {
}
