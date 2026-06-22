CREATE TYPE order_item_inventory_disposition AS ENUM (
    'RELEASE_RESERVED',
    'NO_RESTOCK',
    'RESTOCK',
    'WASTE'
);

CREATE TYPE payment_refund_status AS ENUM ('RECORDED', 'VOIDED');

ALTER TYPE payment_status ADD VALUE IF NOT EXISTS 'REFUND_PENDING' AFTER 'PAID';
ALTER TYPE transaction_reason ADD VALUE IF NOT EXISTS 'SALE_REVERSAL' AFTER 'SALE';

ALTER TABLE orders
    ADD COLUMN refund_due_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0,
    ADD COLUMN refunded_total_snapshot NUMERIC(15,4) NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_refund_due_non_negative
        CHECK (refund_due_snapshot >= 0),
    ADD CONSTRAINT chk_orders_refunded_total_non_negative
        CHECK (refunded_total_snapshot >= 0);

ALTER TABLE order_items
    ADD COLUMN canceled_quantity NUMERIC(15,4) NOT NULL DEFAULT 0;

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_canceled_quantity
        CHECK (canceled_quantity >= 0 AND canceled_quantity <= quantity);

ALTER TABLE kitchen_ticket_lines
    ADD COLUMN canceled_quantity NUMERIC(15,4) NOT NULL DEFAULT 0;

ALTER TABLE kitchen_ticket_lines
    ADD CONSTRAINT chk_kitchen_ticket_lines_canceled_quantity
        CHECK (canceled_quantity >= 0 AND canceled_quantity <= quantity);

CREATE TABLE order_item_cancellations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_item_id UUID NOT NULL,
    quantity NUMERIC(15,4) NOT NULL,
    gross_amount NUMERIC(15,4) NOT NULL,
    reason TEXT NOT NULL,
    kitchen_status_snapshot kitchen_status NOT NULL,
    inventory_disposition order_item_inventory_disposition NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,

    CONSTRAINT fk_oic_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_oic_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_oic_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items(id),
    CONSTRAINT fk_oic_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_oic_quantity CHECK (quantity > 0),
    CONSTRAINT chk_oic_gross_amount CHECK (gross_amount >= 0)
);

CREATE TABLE order_item_cancellation_inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cancellation_id UUID NOT NULL,
    ingredient_stock_id UUID NOT NULL,
    master_ingredient_id UUID NOT NULL,
    quantity_base NUMERIC(18,6) NOT NULL,
    unit_cost_snapshot NUMERIC(18,6),
    inventory_disposition order_item_inventory_disposition NOT NULL,
    inventory_transaction_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_oici_cancellation
        FOREIGN KEY (cancellation_id) REFERENCES order_item_cancellations(id) ON DELETE CASCADE,
    CONSTRAINT fk_oici_stock
        FOREIGN KEY (ingredient_stock_id) REFERENCES ingredient_stocks(id),
    CONSTRAINT fk_oici_ingredient
        FOREIGN KEY (master_ingredient_id) REFERENCES master_ingredients(id),
    CONSTRAINT fk_oici_transaction
        FOREIGN KEY (inventory_transaction_id) REFERENCES inventory_transactions(id),
    CONSTRAINT chk_oici_quantity CHECK (quantity_base > 0)
);

CREATE TABLE payment_refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    cash_session_id UUID NOT NULL,
    device_id UUID NOT NULL,
    amount NUMERIC(15,4) NOT NULL,
    status payment_refund_status NOT NULL DEFAULT 'RECORDED',
    reason TEXT NOT NULL,
    external_reference TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    voided_at TIMESTAMP WITH TIME ZONE,
    voided_by UUID,

    CONSTRAINT fk_payment_refunds_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_payment_refunds_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_payment_refunds_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT fk_payment_refunds_cash_session
        FOREIGN KEY (cash_session_id, restaurant_id)
        REFERENCES cash_sessions(id, restaurant_id),
    CONSTRAINT fk_payment_refunds_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_payment_refunds_voided_by
        FOREIGN KEY (voided_by) REFERENCES users(id),
    CONSTRAINT chk_payment_refunds_amount CHECK (amount > 0)
);

CREATE INDEX idx_oic_order_item
    ON order_item_cancellations (restaurant_id, order_id, order_item_id, created_at);

CREATE INDEX idx_oici_cancellation
    ON order_item_cancellation_inventory (cancellation_id);

CREATE INDEX idx_payment_refunds_order_status
    ON payment_refunds (restaurant_id, order_id, status, created_at);

CREATE INDEX idx_payment_refunds_payment_status
    ON payment_refunds (payment_id, status);
