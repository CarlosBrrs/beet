package com.beet.backend.modules.invoice.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request to register a new invoice.
 * Totals are computed server-side from items — only raw inputs are sent.
 */
public record RegisterInvoiceRequest(
        UUID supplierId,
        String supplierInvoiceNumber,
        LocalDate emissionDate,
        String notes,
        BigDecimal taxPercentage, // Used in PER_INVOICE mode (applied to all items)
        List<InvoiceItemRequest> items) {

    public record InvoiceItemRequest(
            UUID supplierItemId,
            BigDecimal quantityPurchased, // In purchase units (e.g. 2 Bultos)
            BigDecimal unitPricePurchased, // Price per purchase unit (e.g. $50,000/Bulto)
            BigDecimal taxPercentage, // Used in PER_ITEM mode (nullable in PER_INVOICE)
            BigDecimal conversionFactorUsed) { // Snapshot from supplier_item at purchase time
    }
}
