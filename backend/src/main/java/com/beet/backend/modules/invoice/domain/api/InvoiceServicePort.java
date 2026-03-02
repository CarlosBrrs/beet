package com.beet.backend.modules.invoice.domain.api;

import com.beet.backend.modules.invoice.domain.model.InvoiceDomain;

import java.util.UUID;

/**
 * Service port for invoice registration — the atomic orchestrator.
 */
public interface InvoiceServicePort {

    /**
     * Registers an invoice atomically:
     * 1. Persists invoice + items
     * 2. Updates supplier_items.last_cost_base
     * 3. Increases ingredient_stocks.current_stock
     * 4. Creates inventory_transactions with reason=PURCHASE
     */
    InvoiceDomain registerInvoice(InvoiceDomain invoice, UUID restaurantId, UUID userId);
}
