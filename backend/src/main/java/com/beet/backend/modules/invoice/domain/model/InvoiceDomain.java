package com.beet.backend.modules.invoice.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InvoiceDomain {
    private UUID id;
    private UUID ownerId;
    private UUID restaurantId;
    private UUID supplierId;
    private String supplierName; // Populated from JOINs for read queries
    private String supplierInvoiceNumber; // Physical invoice # from supplier
    private LocalDate emissionDate; // When the supplier issued it
    private OffsetDateTime receivedAt; // When registered in Beet
    private BigDecimal subtotal;
    private BigDecimal totalTax;
    private BigDecimal totalAmount;
    private String notes;
    private InvoiceStatus status;
    private OffsetDateTime createdAt;
    private UUID createdBy;

    @Builder.Default
    private List<InvoiceItemDomain> items = new ArrayList<>();
}
