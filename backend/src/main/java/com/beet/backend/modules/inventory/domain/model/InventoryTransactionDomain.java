package com.beet.backend.modules.inventory.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InventoryTransactionDomain {
    private UUID id;
    private UUID ingredientStockId;
    private BigDecimal delta;
    private TransactionReason reason;
    private UUID invoiceId; // nullable: FK to invoices table for PURCHASE transactions
    private BigDecimal previousStock;
    private BigDecimal resultingStock;
    private String notes;
    private OffsetDateTime createdAt;
    private UUID createdBy;
}
