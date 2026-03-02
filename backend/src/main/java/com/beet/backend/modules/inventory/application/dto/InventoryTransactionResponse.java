package com.beet.backend.modules.inventory.application.dto;

import com.beet.backend.modules.inventory.domain.model.TransactionReason;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryTransactionResponse(
                UUID id,
                BigDecimal delta,
                TransactionReason reason,
                UUID invoiceId,
                BigDecimal previousStock,
                BigDecimal resultingStock,
                String notes,
                OffsetDateTime createdAt) {
}
