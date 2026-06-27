package com.beet.backend.modules.invoice.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detailed invoice response with all header fields and enriched items.
 */
public record InvoiceDetailResponse(
        UUID id,
        String supplierName,
        String supplierInvoiceNumber,
        LocalDate emissionDate,
        OffsetDateTime receivedAt,
        BigDecimal subtotal,
        BigDecimal totalTax,
        BigDecimal totalAmount,
        String notes,
        String status,
        List<InvoiceItemDetailResponse> items) {

    public record InvoiceItemDetailResponse(
            UUID id,
            String ingredientName,
            String purchaseUnitName,
            BigDecimal conversionFactorUsed,
            String baseUnitAbbreviation,
            BigDecimal quantityPurchased,
            BigDecimal unitPricePurchased,
            BigDecimal taxPercentage,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal costPerBaseUnit) { // unitPrice / conversionFactor — for display
    }
}
