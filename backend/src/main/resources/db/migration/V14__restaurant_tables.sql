/* =========================================================================
   V14 - Restaurant tables and dine-in order integrity.
   ========================================================================= */

CREATE TABLE restaurant_tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,

    name VARCHAR(120) NOT NULL,
    capacity INTEGER NOT NULL,
    area VARCHAR(120),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL
);

ALTER TABLE restaurant_tables ADD CONSTRAINT fk_restaurant_tables_restaurant
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
ALTER TABLE restaurant_tables ADD CONSTRAINT fk_restaurant_tables_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE restaurant_tables ADD CONSTRAINT fk_restaurant_tables_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE restaurant_tables ADD CONSTRAINT uq_restaurant_tables_id_restaurant
    UNIQUE (id, restaurant_id);
ALTER TABLE restaurant_tables ADD CONSTRAINT chk_restaurant_tables_capacity
    CHECK (capacity > 0);
ALTER TABLE restaurant_tables ADD CONSTRAINT chk_restaurant_tables_sort_order
    CHECK (sort_order >= 0);
ALTER TABLE restaurant_tables ADD CONSTRAINT chk_restaurant_tables_name
    CHECK (BTRIM(name) <> '');

CREATE UNIQUE INDEX uq_restaurant_tables_restaurant_name
    ON restaurant_tables (restaurant_id, LOWER(name));
CREATE INDEX idx_restaurant_tables_restaurant_display
    ON restaurant_tables (restaurant_id, area, sort_order, name);

/* Existing table_id values were provisional UUIDs without a source table. */
UPDATE orders SET table_id = NULL WHERE table_id IS NOT NULL;

ALTER TABLE orders ADD CONSTRAINT fk_orders_table_restaurant
    FOREIGN KEY (table_id, restaurant_id)
    REFERENCES restaurant_tables(id, restaurant_id)
    ON DELETE RESTRICT;

CREATE UNIQUE INDEX uq_orders_open_table
    ON orders (restaurant_id, table_id)
    WHERE order_status = 'OPEN' AND table_id IS NOT NULL;
