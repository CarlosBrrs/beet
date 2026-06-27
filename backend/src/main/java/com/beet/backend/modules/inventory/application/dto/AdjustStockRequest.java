package com.beet.backend.modules.inventory.application.dto;

import com.beet.backend.modules.inventory.domain.model.TransactionReason;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustStockRequest(
        @NotNull AdjustmentMode mode,
        @NotNull BigDecimal value,
        @NotNull TransactionReason reason, // limited to ADJUSTMENT, WASTE, CORRECTION
        String notes) {
    public enum AdjustmentMode {
        REPLACE,
        DELTA
    }
}
