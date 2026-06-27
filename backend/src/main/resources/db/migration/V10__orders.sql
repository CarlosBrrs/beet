/* =========================================================================
   V10 — Orders (MVP)
   Purpose: Create orders for sellable products (flat or with recipe).
   Payments and KDS are out of scope now, but statuses are stored to scale.
   Taxes are price-included (gross) for Colombia-style pricing.
   ========================================================================= */

/* =========================================================================
   Enums
   ========================================================================= */
CREATE TYPE order_status AS ENUM (
    'OPEN',       -- Order created and still in progress
    'COMPLETED',  -- Done (future: depends on kitchen + payment)
    'CANCELED'    -- Voided by staff
);

CREATE TYPE kitchen_status AS ENUM (
    'PENDING',    -- Created, not yet accepted by kitchen
    'ACCEPTED',   -- Kitchen accepted the order
    'READY',      -- Finished and ready
    'SERVED'      -- Delivered to customer
);

CREATE TYPE payment_status AS ENUM (
    'UNPAID',          -- No payment yet
    'PARTIALLY_PAID',  -- Partial payment received
    'PAID',            -- Fully paid
    'REFUNDED'         -- Refunded after payment
);

CREATE TYPE service_type AS ENUM (
    'DINE_IN',
    'TAKEOUT',
    'DELIVERY'
);

/* =========================================================================
   1. orders (Header)
   ========================================================================= */
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,                 /* Tenant scoping */

    order_status order_status NOT NULL DEFAULT 'OPEN',
    kitchen_status kitchen_status NOT NULL DEFAULT 'PENDING',
    payment_status payment_status NOT NULL DEFAULT 'UNPAID',

    service_type service_type NOT NULL,          /* DINE_IN / TAKEOUT / DELIVERY */
    table_id UUID,                               /* Mocked UUID for now (no FK) */
    customer_name TEXT,                          /* Optional customer identifier */

    /* Snapshot from restaurant config at creation time */
    prepayment_required_snapshot BOOLEAN NOT NULL DEFAULT FALSE,  /* If true, kitchen should wait for payment */

        /* Taxes are price-included (gross). No breakdown stored in MVP.
             TODO(taxes): When scaling, keep restaurant_taxes as the CONFIG source
             (active tax definitions per restaurant) and add order_item_taxes to
             SNAPSHOT the applied taxes at order time:
                 - order_item_taxes(order_item_id, tax_id, tax_rate_snapshot,
                     tax_base_snapshot, tax_amount_snapshot)
             Optionally add order_taxes for header-level summaries.
             Snapshot is required so historical orders stay stable if taxes change.
             Net/tax from gross can be derived as:
                 tax_amount = gross * rate / (100 + rate)
                 net_amount = gross - tax_amount
             Keep the existing *_snapshot fields as gross totals for quick reads. */
    tax_rate_snapshot NUMERIC(5,2) NOT NULL DEFAULT 0,            /* % applied to gross price */

    subtotal_gross_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,     /* Sum of item subtotals (gross) */
    tax_amount_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,         /* gross * rate / (100 + rate) */
    total_gross_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,        /* Subtotal + tax (still gross) */

    notes TEXT,

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL
);

/* =========================================================================
   2. order_items (Detail)
   ========================================================================= */
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,

    item_id UUID NOT NULL,                        /* FK -> items (SALEABLE_PRODUCT) */
    submenu_node_id UUID,                         /* FK -> submenu_nodes (nullable) */

    /* Snapshots to keep history stable if item changes later */
    item_name_snapshot TEXT NOT NULL,
    unit_price_snapshot NUMERIC(15,4) NOT NULL,   /* Gross unit price at order time */
    theoretical_cost_snapshot NUMERIC(15,4),      /* Cost per unit at order time */

    quantity NUMERIC(15,4) NOT NULL,
    subtotal_gross_snapshot NUMERIC(15,4) NOT NULL,        /* quantity * unit_price_snapshot */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL
);

/* =========================================================================
   3. order_taxes (Header-level tax snapshot)
   =========================================================================
   Each row represents one tax applied to the entire order at the time it
   was created or recalculated. This is a snapshot of the tax definition
   (name + rate) so orders remain stable if restaurant taxes change later.
   Source configuration: restaurant_taxes -> taxes.
   ========================================================================= */
CREATE TABLE order_taxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    tax_id UUID,                               /* FK -> taxes (nullable if deleted later) */

    tax_name_snapshot VARCHAR(100) NOT NULL,   /* Tax name at order time */
    tax_rate_snapshot NUMERIC(10,2) NOT NULL,  /* % at order time */

    tax_base_snapshot NUMERIC(15,4) NOT NULL,  /* Gross base used for tax calc */
    tax_amount_snapshot NUMERIC(15,4) NOT NULL,/* gross * rate / (100 + rate) */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL
);

/* =========================================================================
   4. order_item_taxes (Line-level tax snapshot)
   =========================================================================
   Each row represents one tax applied to a specific order item. This is the
   scalable form for future per-item tax rules. Source configuration is still
   restaurant_taxes -> taxes, but values are snapshotted here.
   ========================================================================= */
CREATE TABLE order_item_taxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID NOT NULL,
    tax_id UUID,                                /* FK -> taxes (nullable if deleted later) */

    tax_name_snapshot VARCHAR(100) NOT NULL,    /* Tax name at order time */
    tax_rate_snapshot NUMERIC(10,2) NOT NULL,   /* % at order time */

    tax_base_snapshot NUMERIC(15,4) NOT NULL,   /* Gross base for this line */
    tax_amount_snapshot NUMERIC(15,4) NOT NULL, /* gross * rate / (100 + rate) */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL
);

/* =========================================================================
   CONSTRAINTS & FOREIGN KEYS
   ========================================================================= */

/* orders */
ALTER TABLE orders ADD CONSTRAINT fk_orders_restaurant
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id);

/* order_items */
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_item
    FOREIGN KEY (item_id) REFERENCES items(id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_submenu_node
    FOREIGN KEY (submenu_node_id) REFERENCES submenu_nodes(id) ON DELETE SET NULL;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id);

/* order_taxes */
ALTER TABLE order_taxes ADD CONSTRAINT fk_order_taxes_order
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;
ALTER TABLE order_taxes ADD CONSTRAINT fk_order_taxes_tax
    FOREIGN KEY (tax_id) REFERENCES taxes(id) ON DELETE SET NULL;
ALTER TABLE order_taxes ADD CONSTRAINT fk_order_taxes_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);

/* order_item_taxes */
ALTER TABLE order_item_taxes ADD CONSTRAINT fk_order_item_taxes_order_item
    FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE;
ALTER TABLE order_item_taxes ADD CONSTRAINT fk_order_item_taxes_tax
    FOREIGN KEY (tax_id) REFERENCES taxes(id) ON DELETE SET NULL;
ALTER TABLE order_item_taxes ADD CONSTRAINT fk_order_item_taxes_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);

/* =========================================================================
   Indexes
   ========================================================================= */
CREATE INDEX idx_orders_restaurant_status_created
    ON orders (restaurant_id, order_status, created_at DESC);
CREATE INDEX idx_orders_restaurant_kitchen
    ON orders (restaurant_id, kitchen_status, created_at DESC);
CREATE INDEX idx_order_items_order
    ON order_items (order_id);
CREATE INDEX idx_order_items_item
    ON order_items (item_id);
CREATE INDEX idx_order_taxes_order
    ON order_taxes (order_id);
CREATE INDEX idx_order_item_taxes_item
    ON order_item_taxes (order_item_id);
