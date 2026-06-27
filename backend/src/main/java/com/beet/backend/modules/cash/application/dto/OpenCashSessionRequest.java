package com.beet.backend.modules.cash.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OpenCashSessionRequest(
        @NotNull UUID cashRegisterId,
        @NotNull @DecimalMin("0") BigDecimal openingAmount,
        String notes) {
}
