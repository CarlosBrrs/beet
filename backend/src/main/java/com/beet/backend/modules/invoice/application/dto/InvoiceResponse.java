package com.beet.backend.modules.invoice.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response for invoice list view (summary — no item details).
 */
public record InvoiceResponse(
        UUID id,
        String supplierName,
        String supplierInvoiceNumber,
        LocalDate emissionDate,
        OffsetDateTime receivedAt,
        BigDecimal totalAmount,
        int itemCount,
        String status) {
}
