/* =========================================================================
   V16 - Orders, POS, kitchen tickets, payments, bills and sales inventory.
   ========================================================================= */

ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'DRAFT' BEFORE 'OPEN';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'AWAITING_PAYMENT' BEFORE 'OPEN';
ALTER TYPE kitchen_status ADD VALUE IF NOT EXISTS 'NOT_SENT' BEFORE 'PENDING';
ALTER TYPE kitchen_status ADD VALUE IF NOT EXISTS 'PREPARING' BEFORE 'READY';
ALTER TYPE kitchen_status ADD VALUE IF NOT EXISTS 'PARTIALLY_READY' BEFORE 'READY';

CREATE TYPE order_line_type AS ENUM ('PRODUCT', 'TEMPLATE');
CREATE TYPE kitchen_ticket_status AS ENUM ('PENDING', 'PREPARING', 'READY', 'CANCELED');
CREATE TYPE delivery_status AS ENUM ('NOT_APPLICABLE', 'PENDING_DISPATCH', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELED');
CREATE TYPE inventory_reservation_status AS ENUM ('ACTIVE', 'CONSUMED', 'RELEASED', 'CANCELED');
CREATE TYPE payment_method_type AS ENUM ('CASH', 'DEBIT_CARD', 'CREDIT_CARD', 'NEQUI', 'DAVIPLATA', 'INTERNAL_CREDIT', 'OTHER');
CREATE TYPE payment_provider AS ENUM ('MANUAL');
CREATE TYPE payment_record_status AS ENUM ('RECORDED', 'VOIDED', 'REFUNDED');
CREATE TYPE bill_payment_status AS ENUM ('UNPAID', 'PARTIALLY_PAID', 'PAID');
CREATE TYPE bill_split_mode AS ENUM ('ITEM_QUANTITY', 'AMOUNT', 'PERCENTAGE', 'EVENLY');
CREATE TYPE adjustment_scope AS ENUM ('ORDER', 'ITEM');
CREATE TYPE adjustment_type AS ENUM ('DISCOUNT', 'COMP', 'MANUAL_PRICE_OVERRIDE');
CREATE TYPE adjustment_mode AS ENUM ('AMOUNT', 'PERCENT');
CREATE TYPE catalog_reference_type AS ENUM ('PRODUCT', 'TEMPLATE');
CREATE TYPE catalog_availability_status AS ENUM ('AVAILABLE', 'UNAVAILABLE');

ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_cash_session_restaurant;
ALTER TABLE orders RENAME COLUMN cash_session_id TO origin_cash_session_id;
ALTER TABLE orders ALTER COLUMN origin_cash_session_id DROP NOT NULL;

ALTER TABLE orders
    ADD COLUMN operation_mode_snapshot operation_mode_enum NOT NULL DEFAULT 'POSTPAID',
    ADD COLUMN origin_device_id UUID,
    ADD COLUMN customer_phone VARCHAR(40),
    ADD COLUMN delivery_contact_name VARCHAR(160),
    ADD COLUMN delivery_phone VARCHAR(40),
    ADD COLUMN delivery_address TEXT,
    ADD COLUMN delivery_notes TEXT,
    ADD COLUMN delivery_fee NUMERIC(15,4) NOT NULL DEFAULT 0,
    ADD COLUMN delivery_status delivery_status NOT NULL DEFAULT 'NOT_APPLICABLE',
    ADD COLUMN tip_total_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,
    ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN canceled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN cancel_reason TEXT;

ALTER TABLE orders ADD CONSTRAINT fk_orders_origin_cash_session_restaurant
    FOREIGN KEY (origin_cash_session_id, restaurant_id)
    REFERENCES cash_sessions(id, restaurant_id);
ALTER TABLE orders ADD CONSTRAINT chk_orders_delivery_fee CHECK (delivery_fee >= 0);
ALTER TABLE orders ADD CONSTRAINT chk_orders_tip_total CHECK (tip_total_snapshot >= 0);

ALTER TABLE order_items DROP CONSTRAINT IF EXISTS fk_order_items_item;
ALTER TABLE order_items ALTER COLUMN item_id DROP NOT NULL;
ALTER TABLE order_items
    ADD COLUMN line_type order_line_type NOT NULL DEFAULT 'PRODUCT',
    ADD COLUMN template_id UUID,
    ADD COLUMN notes TEXT;

ALTER TABLE order_items ADD CONSTRAINT fk_order_items_item_restaurant
    FOREIGN KEY (item_id) REFERENCES items(id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_template
    FOREIGN KEY (template_id) REFERENCES templates(id);
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_line_reference CHECK (
    (line_type = 'PRODUCT' AND item_id IS NOT NULL AND template_id IS NULL)
    OR
    (line_type = 'TEMPLATE' AND template_id IS NOT NULL)
);

CREATE TABLE order_item_template_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID NOT NULL,
    template_slot_id UUID,
    slot_name_snapshot VARCHAR(160) NOT NULL,
    min_selection_snapshot INTEGER NOT NULL,
    max_selection_snapshot INTEGER NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_oit_slots_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE
);

CREATE TABLE order_item_template_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_template_slot_id UUID NOT NULL,
    slot_option_id UUID,
    item_id UUID NOT NULL,
    item_name_snapshot VARCHAR(160) NOT NULL,
    quantity NUMERIC(15,4) NOT NULL,
    surcharge_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,
    theoretical_cost_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_oit_options_slot FOREIGN KEY (order_item_template_slot_id) REFERENCES order_item_template_slots(id) ON DELETE CASCADE,
    CONSTRAINT fk_oit_options_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT chk_oit_options_quantity CHECK (quantity > 0),
    CONSTRAINT chk_oit_options_surcharge CHECK (surcharge_snapshot >= 0)
);

CREATE TABLE kitchen_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    status kitchen_ticket_status NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    sent_by UUID NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    started_by UUID,
    ready_at TIMESTAMP WITH TIME ZONE,
    ready_by UUID,
    canceled_at TIMESTAMP WITH TIME ZONE,
    canceled_by UUID,
    notes TEXT,
    CONSTRAINT fk_kitchen_tickets_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_kitchen_tickets_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_kitchen_tickets_sent_by FOREIGN KEY (sent_by) REFERENCES users(id),
    CONSTRAINT fk_kitchen_tickets_started_by FOREIGN KEY (started_by) REFERENCES users(id),
    CONSTRAINT fk_kitchen_tickets_ready_by FOREIGN KEY (ready_by) REFERENCES users(id),
    CONSTRAINT fk_kitchen_tickets_canceled_by FOREIGN KEY (canceled_by) REFERENCES users(id)
);

CREATE TABLE kitchen_ticket_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kitchen_ticket_id UUID NOT NULL,
    order_item_id UUID NOT NULL,
    quantity NUMERIC(15,4) NOT NULL,
    item_name_snapshot VARCHAR(160) NOT NULL,
    notes TEXT,
    CONSTRAINT fk_kitchen_ticket_lines_ticket FOREIGN KEY (kitchen_ticket_id) REFERENCES kitchen_tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_kitchen_ticket_lines_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE,
    CONSTRAINT chk_kitchen_ticket_lines_quantity CHECK (quantity > 0)
);

CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_item_id UUID NOT NULL,
    ingredient_stock_id UUID NOT NULL,
    master_ingredient_id UUID NOT NULL,
    quantity_base NUMERIC(15,4) NOT NULL,
    status inventory_reservation_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT fk_inventory_reservations_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_inventory_reservations_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_reservations_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_reservations_stock FOREIGN KEY (ingredient_stock_id) REFERENCES ingredient_stocks(id),
    CONSTRAINT fk_inventory_reservations_master FOREIGN KEY (master_ingredient_id) REFERENCES master_ingredients(id),
    CONSTRAINT fk_inventory_reservations_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_inventory_reservations_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_inventory_reservations_quantity CHECK (quantity_base > 0)
);

CREATE TABLE order_item_consumptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_item_id UUID NOT NULL,
    ingredient_stock_id UUID NOT NULL,
    master_ingredient_id UUID NOT NULL,
    quantity_base NUMERIC(15,4) NOT NULL,
    unit_cost_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,
    inventory_transaction_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT fk_order_item_consumptions_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_order_item_consumptions_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_consumptions_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_consumptions_stock FOREIGN KEY (ingredient_stock_id) REFERENCES ingredient_stocks(id),
    CONSTRAINT fk_order_item_consumptions_master FOREIGN KEY (master_ingredient_id) REFERENCES master_ingredients(id),
    CONSTRAINT fk_order_item_consumptions_tx FOREIGN KEY (inventory_transaction_id) REFERENCES inventory_transactions(id),
    CONSTRAINT fk_order_item_consumptions_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_order_item_consumptions_quantity CHECK (quantity_base > 0)
);

ALTER TABLE inventory_transactions
    ADD COLUMN order_id UUID REFERENCES orders(id),
    ADD COLUMN order_item_id UUID REFERENCES order_items(id);

CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type payment_method_type NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    requires_reference BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT fk_payment_methods_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_payment_methods_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_payment_methods_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT uq_payment_methods_code UNIQUE (restaurant_id, code),
    CONSTRAINT chk_payment_methods_sort_order CHECK (sort_order >= 0)
);

CREATE TABLE payment_intents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    provider payment_provider NOT NULL DEFAULT 'MANUAL',
    provider_reference VARCHAR(160),
    amount NUMERIC(15,4) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'CREATED',
    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_payment_intents_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_payment_intents_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT chk_payment_intents_amount CHECK (amount >= 0)
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_bill_id UUID,
    payment_method_id UUID NOT NULL,
    payment_intent_id UUID,
    cash_session_id UUID NOT NULL,
    device_id UUID NOT NULL,
    amount NUMERIC(15,4) NOT NULL,
    tip_amount NUMERIC(15,4) NOT NULL DEFAULT 0,
    status payment_record_status NOT NULL DEFAULT 'RECORDED',
    external_reference VARCHAR(160),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    voided_at TIMESTAMP WITH TIME ZONE,
    voided_by UUID,
    CONSTRAINT fk_payments_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_payments_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
    CONSTRAINT fk_payments_intent FOREIGN KEY (payment_intent_id) REFERENCES payment_intents(id),
    CONSTRAINT fk_payments_session_restaurant FOREIGN KEY (cash_session_id, restaurant_id) REFERENCES cash_sessions(id, restaurant_id),
    CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_payments_voided_by FOREIGN KEY (voided_by) REFERENCES users(id),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_tip CHECK (tip_amount >= 0)
);

CREATE TABLE order_bills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    label VARCHAR(120) NOT NULL,
    split_mode bill_split_mode NOT NULL,
    subtotal_gross_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,
    tip_total_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,
    total_paid_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,
    payment_status bill_payment_status NOT NULL DEFAULT 'UNPAID',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT fk_order_bills_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_order_bills_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_bills_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_order_bills_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

ALTER TABLE payments ADD CONSTRAINT fk_payments_bill
    FOREIGN KEY (order_bill_id) REFERENCES order_bills(id);

CREATE TABLE order_bill_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_bill_id UUID NOT NULL,
    order_item_id UUID,
    quantity NUMERIC(15,4),
    amount NUMERIC(15,4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_order_bill_allocations_bill FOREIGN KEY (order_bill_id) REFERENCES order_bills(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_bill_allocations_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT chk_order_bill_allocations_quantity CHECK (quantity IS NULL OR quantity > 0),
    CONSTRAINT chk_order_bill_allocations_amount CHECK (amount >= 0)
);

CREATE TABLE order_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_item_id UUID,
    scope adjustment_scope NOT NULL,
    type adjustment_type NOT NULL,
    mode adjustment_mode NOT NULL,
    value NUMERIC(15,4) NOT NULL,
    reason TEXT NOT NULL,
    approved_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT fk_order_adjustments_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_order_adjustments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_adjustments_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_order_adjustments_approved_by FOREIGN KEY (approved_by) REFERENCES users(id),
    CONSTRAINT fk_order_adjustments_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_order_adjustments_value CHECK (value >= 0)
);

CREATE TABLE catalog_availability_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    reference_type catalog_reference_type NOT NULL,
    reference_id UUID NOT NULL,
    status catalog_availability_status NOT NULL,
    reason TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT fk_catalog_availability_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_catalog_availability_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_orders_rest_filters
    ON orders (restaurant_id, order_status, payment_status, kitchen_status, created_at DESC);
CREATE INDEX idx_orders_rest_service_table
    ON orders (restaurant_id, service_type, table_id);
CREATE INDEX idx_orders_origin_cash
    ON orders (origin_cash_session_id);
CREATE INDEX idx_order_items_order_line
    ON order_items (order_id, line_type);
CREATE INDEX idx_kitchen_tickets_rest_status
    ON kitchen_tickets (restaurant_id, status, sent_at DESC);
CREATE INDEX idx_kitchen_tickets_order
    ON kitchen_tickets (order_id, sent_at DESC);
CREATE INDEX idx_inventory_reservations_item_status
    ON inventory_reservations (order_item_id, status);
CREATE INDEX idx_inventory_reservations_stock_status
    ON inventory_reservations (ingredient_stock_id, status);
CREATE INDEX idx_order_item_consumptions_item
    ON order_item_consumptions (order_item_id);
CREATE INDEX idx_payments_order_status
    ON payments (order_id, status);
CREATE INDEX idx_payments_method
    ON payments (payment_method_id);
CREATE INDEX idx_order_bills_order_status
    ON order_bills (order_id, payment_status);
CREATE INDEX idx_catalog_availability_active
    ON catalog_availability_overrides (restaurant_id, reference_type, reference_id, status, expires_at);
