/* =========================================================================
   Invoices & Invoice Items
   =========================================================================
   Phase 3 of inventory management: supplier purchase registration.
   Tracks invoices, updates ingredient costs and stock atomically.
   ========================================================================= */

/* =========================================================================
   1. invoices (Header — the legal document from the supplier)
   ========================================================================= */
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,                        /* Tenant scoping — same owner across restaurants */
    restaurant_id UUID NOT NULL,                   /* Which restaurant received the goods */
    supplier_id UUID NOT NULL,                     /* Who sold the goods */
    supplier_invoice_number VARCHAR(50) NOT NULL,  /* The physical invoice # from the supplier */
    emission_date DATE NOT NULL,                   /* When the supplier issued it */
    received_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, /* When it was registered in Beet */

    /* Totals — computed server-side from item lines */
    subtotal NUMERIC(15,4) NOT NULL,
    total_tax NUMERIC(15,4) NOT NULL,
    total_amount NUMERIC(15,4) NOT NULL,

    notes TEXT,
    status VARCHAR(20) DEFAULT 'COMPLETED' NOT NULL, /* COMPLETED | VOIDED (future) */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,

    /* Prevent duplicate registration of the same supplier invoice */
    UNIQUE(restaurant_id, supplier_id, supplier_invoice_number)
);

/* =========================================================================
   2. invoice_items (Detail — each line of the invoice)
   ========================================================================= */
CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL,
    supplier_item_id UUID NOT NULL,                /* FK → supplier_items (the offering purchased) */

    quantity_purchased NUMERIC(15,4) NOT NULL,     /* In purchase units (e.g. 2 Bultos) */
    unit_price_purchased NUMERIC(15,4) NOT NULL,   /* Price per PURCHASE UNIT (e.g. $50,000/Bulto) */

    tax_percentage NUMERIC(5,2) DEFAULT 19.00 NOT NULL,  /* Per-item tax rate (0 for exempt) */
    subtotal NUMERIC(15,4) NOT NULL,               /* quantity × unit_price */
    tax_amount NUMERIC(15,4) NOT NULL,             /* subtotal × tax_percentage / 100 */

    conversion_factor_used NUMERIC(15,6) NOT NULL  /* Snapshot of supplier_item.conversion_factor at purchase time */
);

/* =========================================================================
   CONSTRAINTS & FOREIGN KEYS
   ========================================================================= */

/* invoices */
ALTER TABLE invoices ADD CONSTRAINT fk_inv_owner
    FOREIGN KEY (owner_id) REFERENCES users(id);
ALTER TABLE invoices ADD CONSTRAINT fk_inv_restaurant
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
ALTER TABLE invoices ADD CONSTRAINT fk_inv_supplier
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id);
ALTER TABLE invoices ADD CONSTRAINT fk_inv_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);

/* invoice_items */
ALTER TABLE invoice_items ADD CONSTRAINT fk_ii_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE;
ALTER TABLE invoice_items ADD CONSTRAINT fk_ii_supplier_item
    FOREIGN KEY (supplier_item_id) REFERENCES supplier_items(id);

/* =========================================================================
   MIGRATION: Replace generic reference_id with explicit invoice_id FK
   in inventory_transactions
   ========================================================================= */
ALTER TABLE inventory_transactions DROP COLUMN reference_id;
ALTER TABLE inventory_transactions ADD COLUMN invoice_id UUID REFERENCES invoices(id);
