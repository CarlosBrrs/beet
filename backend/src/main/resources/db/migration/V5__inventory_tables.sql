/* =========================================================================
   Inventory Stock & Transactions
   ========================================================================= */

/* Transaction reason enum — matches Java TransactionReason enum */
CREATE TYPE transaction_reason AS ENUM (
    'INITIAL',      -- First stock activation in a restaurant
    'ADJUSTMENT',   -- Generic manual correction (e.g. unregistered stock found)
    'WASTE',        -- Product spoiled, damaged, or discarded
    'CORRECTION',   -- Physical inventory count reconciliation
    'PURCHASE',     -- Restock via supplier invoice (Phase 3)
    'SALE'          -- Deducted by a customer order (future)
);

/* =========================================================================
   1. ingredient_stocks — Activates a master ingredient in a restaurant
   ========================================================================= */
CREATE TABLE ingredient_stocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    master_ingredient_id UUID NOT NULL,
    restaurant_id UUID NOT NULL,
    current_stock NUMERIC(15,4) NOT NULL DEFAULT 0,
    min_stock NUMERIC(15,4) NOT NULL DEFAULT 0,

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   2. inventory_transactions — Immutable log of every stock movement
   ========================================================================= */
CREATE TABLE inventory_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ingredient_stock_id UUID NOT NULL,
    delta NUMERIC(15,4) NOT NULL,               /* Signed: +50 (in), -20 (out) */
    reason transaction_reason NOT NULL,
    reference_id UUID,                           /* Nullable: invoice_id, order_id, or null for manual */
    previous_stock NUMERIC(15,4) NOT NULL,
    resulting_stock NUMERIC(15,4) NOT NULL,
    notes TEXT,

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL
);

/* =========================================================================
   CONSTRAINTS & FOREIGN KEYS
   ========================================================================= */

/* ingredient_stocks */
ALTER TABLE ingredient_stocks ADD CONSTRAINT fk_is_master_ingredient
    FOREIGN KEY (master_ingredient_id) REFERENCES master_ingredients(id);
ALTER TABLE ingredient_stocks ADD CONSTRAINT fk_is_restaurant
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
ALTER TABLE ingredient_stocks ADD CONSTRAINT fk_is_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE ingredient_stocks ADD CONSTRAINT fk_is_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE ingredient_stocks ADD CONSTRAINT fk_is_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES users(id);
ALTER TABLE ingredient_stocks ADD CONSTRAINT uq_is_ingredient_restaurant
    UNIQUE (master_ingredient_id, restaurant_id);

/* inventory_transactions */
ALTER TABLE inventory_transactions ADD CONSTRAINT fk_it_ingredient_stock
    FOREIGN KEY (ingredient_stock_id) REFERENCES ingredient_stocks(id);
ALTER TABLE inventory_transactions ADD CONSTRAINT fk_it_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
