package com.beet.backend.modules.invoice.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response for supplier items used in the invoice form's item picker.
 * Includes ingredient name and base unit info for conversion display.
 */
public record SupplierItemForInvoiceResponse(
        UUID id,
        String brandName,
        String purchaseUnitName,
        BigDecimal conversionFactor,
        BigDecimal lastCostBase,
        UUID masterIngredientId,
        String ingredientName,
        String baseUnitAbbreviation) {
}
