package com.beet.backend.modules.invoice.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InvoiceItemDomain {
    private UUID id;
    private UUID invoiceId;
    private UUID supplierItemId;
    private BigDecimal quantityPurchased; // In purchase units (e.g. 2 Bultos)
    private BigDecimal unitPricePurchased; // Price per purchase unit (e.g. $50,000/Bulto)
    private BigDecimal taxPercentage; // e.g. 19.00
    private BigDecimal subtotal; // quantity × unitPrice
    private BigDecimal taxAmount; // subtotal × taxPercentage / 100
    private BigDecimal conversionFactorUsed; // Snapshot from supplier_item at purchase time
}
