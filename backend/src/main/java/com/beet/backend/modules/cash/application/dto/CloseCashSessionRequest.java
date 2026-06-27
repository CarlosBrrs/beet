package com.beet.backend.modules.cash.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CloseCashSessionRequest(
        @NotNull @DecimalMin("0") BigDecimal countedCash,
        String differenceReason,
        String notes) {
}
