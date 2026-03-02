package com.beet.backend.modules.inventory.domain.model;

/**
 * Transaction reason enum — matches DB 'transaction_reason' enum.
 *
 * INITIAL — First stock activation in a restaurant
 * ADJUSTMENT — Generic manual correction (e.g. unregistered stock found,
 * positive or negative)
 * WASTE — Product spoiled, damaged, or discarded (always negative delta)
 * CORRECTION — Physical inventory count reconciliation (positive or negative)
 * PURCHASE — Restock via supplier invoice (Phase 3, always positive delta)
 * SALE — Deducted by a customer order (future, always negative delta)
 */
public enum TransactionReason {
    INITIAL,
    ADJUSTMENT,
    WASTE,
    CORRECTION,
    PURCHASE,
    SALE
}
