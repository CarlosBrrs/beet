package com.beet.backend.modules.invoice.domain.usecase;

import com.beet.backend.modules.ingredient.domain.model.SupplierItemDomain;
import com.beet.backend.modules.inventory.domain.model.InventoryStockDomain;
import com.beet.backend.modules.inventory.domain.model.InventoryTransactionDomain;
import com.beet.backend.modules.inventory.domain.model.TransactionReason;
import com.beet.backend.modules.inventory.domain.spi.InventoryPersistencePort;
import com.beet.backend.modules.invoice.domain.api.InvoiceServicePort;
import com.beet.backend.modules.invoice.domain.model.InvoiceDomain;
import com.beet.backend.modules.invoice.domain.model.InvoiceItemDomain;
import com.beet.backend.modules.invoice.domain.model.InvoiceStatus;
import com.beet.backend.modules.invoice.domain.spi.InvoicePersistencePort;
import com.beet.backend.modules.invoice.domain.spi.SupplierItemQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Core invoice registration use case.
 * Executes a single atomic transaction that:
 * 1. Computes and persists the invoice + items
 * 2. Updates supplier_items.last_cost_base for each item
 * 3. Increases ingredient_stocks.current_stock
 * 4. Creates inventory_transactions with reason=PURCHASE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceUseCase implements InvoiceServicePort {

    private final InvoicePersistencePort invoicePersistence;
    private final SupplierItemQueryPort supplierItemQuery;
    private final InventoryPersistencePort inventoryPersistence;

    @Override
    @Transactional
    public InvoiceDomain registerInvoice(InvoiceDomain invoice, UUID restaurantId, UUID userId) {
        // ── Step 1: Compute item subtotals & tax amounts ──
        BigDecimal headerSubtotal = BigDecimal.ZERO;
        BigDecimal headerTotalTax = BigDecimal.ZERO;

        for (InvoiceItemDomain item : invoice.getItems()) {
            // Subtotal = quantity × unit_price
            BigDecimal subtotal = item.getQuantityPurchased()
                    .multiply(item.getUnitPricePurchased())
                    .setScale(4, RoundingMode.HALF_UP);
            item.setSubtotal(subtotal);

            // Tax amount = subtotal × tax_percentage / 100
            BigDecimal taxAmount = subtotal
                    .multiply(item.getTaxPercentage())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            item.setTaxAmount(taxAmount);

            headerSubtotal = headerSubtotal.add(subtotal);
            headerTotalTax = headerTotalTax.add(taxAmount);
        }

        // Compute header totals
        invoice.setSubtotal(headerSubtotal);
        invoice.setTotalTax(headerTotalTax);
        invoice.setTotalAmount(headerSubtotal.add(headerTotalTax));
        invoice.setStatus(InvoiceStatus.COMPLETED);
        invoice.setCreatedBy(userId);

        // ── Step 2: Persist invoice + items ──
        InvoiceDomain saved = invoicePersistence.save(invoice);
        UUID invoiceId = saved.getId();

        log.info("Invoice {} persisted for restaurant {} with {} items",
                invoiceId, restaurantId, invoice.getItems().size());

        // ── Step 3: For each item, update costs, stock, and create transaction ──
        for (InvoiceItemDomain item : saved.getItems()) {
            processInvoiceItem(item, invoiceId, restaurantId, userId);
        }

        log.info("Invoice {} fully processed: costs updated, stock increased, transactions logged", invoiceId);
        return saved;
    }

    /**
     * Processes a single invoice item:
     * a) Loads the supplier item to get masterIngredientId
     * b) Updates last_cost_base
     * c) Finds/auto-creates ingredient stock
     * d) Increases current_stock
     * e) Logs inventory transaction
     */
    private void processInvoiceItem(InvoiceItemDomain item, UUID invoiceId,
            UUID restaurantId, UUID userId) {
        // a) Load supplier item
        SupplierItemDomain supplierItem = supplierItemQuery.findById(item.getSupplierItemId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Supplier item not found: " + item.getSupplierItemId()));

        UUID masterIngredientId = supplierItem.getMasterIngredientId();
        BigDecimal conversionFactor = item.getConversionFactorUsed();

        // b) Compute new base cost and update supplier_items.last_cost_base
        // cost_per_base_unit = unit_price / conversion_factor
        BigDecimal newCostBase = item.getUnitPricePurchased()
                .divide(conversionFactor, 6, RoundingMode.HALF_UP);
        supplierItemQuery.updateLastCostBase(item.getSupplierItemId(), newCostBase);

        log.debug("Updated last_cost_base for supplier_item {} → {}", item.getSupplierItemId(), newCostBase);

        // c) Find or auto-activate ingredient stock
        // base_quantity = quantity_purchased × conversion_factor
        BigDecimal baseQuantity = item.getQuantityPurchased()
                .multiply(conversionFactor)
                .setScale(4, RoundingMode.HALF_UP);

        InventoryStockDomain stock = inventoryPersistence
                .findStockByIngredientAndRestaurant(masterIngredientId, restaurantId)
                .orElseGet(() -> {
                    // Auto-activate: create stock entry with current_stock = 0
                    log.info("Auto-activating ingredient {} in restaurant {}", masterIngredientId, restaurantId);
                    InventoryStockDomain newStock = InventoryStockDomain.builder()
                            .masterIngredientId(masterIngredientId)
                            .restaurantId(restaurantId)
                            .currentStock(BigDecimal.ZERO)
                            .minStock(BigDecimal.ZERO)
                            .build();
                    return inventoryPersistence.saveStock(newStock);
                });

        // d) Increase current_stock
        BigDecimal previousStock = stock.getCurrentStock();
        BigDecimal resultingStock = previousStock.add(baseQuantity);
        inventoryPersistence.updateCurrentStock(stock.getId(), resultingStock, userId);

        // e) Create inventory transaction
        InventoryTransactionDomain transaction = InventoryTransactionDomain.builder()
                .ingredientStockId(stock.getId())
                .delta(baseQuantity)
                .reason(TransactionReason.PURCHASE)
                .invoiceId(invoiceId)
                .previousStock(previousStock)
                .resultingStock(resultingStock)
                .notes("Invoice purchase")
                .createdBy(userId)
                .build();
        inventoryPersistence.saveTransaction(transaction);

        log.debug("Stock {} updated: {} → {} (+{} base units)",
                stock.getId(), previousStock, resultingStock, baseQuantity);
    }
}
