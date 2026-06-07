ALTER TABLE items
    ADD COLUMN sellable_units_per_batch INTEGER;

UPDATE items
   SET sellable_units_per_batch = 1
 WHERE class = 'PRODUCT';

ALTER TABLE items
    ADD CONSTRAINT chk_items_sellable_units
    CHECK (
        (class = 'PREPARATION' AND sellable_units_per_batch IS NULL)
        OR
        (class = 'PRODUCT' AND sellable_units_per_batch IS NOT NULL AND sellable_units_per_batch >= 1)
    );

ALTER TABLE supplier_items
    ALTER COLUMN last_cost_base TYPE NUMERIC(18,6);

ALTER TABLE ingredient_stocks
    ALTER COLUMN current_stock TYPE NUMERIC(18,6),
    ALTER COLUMN min_stock TYPE NUMERIC(18,6);

ALTER TABLE inventory_transactions
    ALTER COLUMN delta TYPE NUMERIC(18,6),
    ALTER COLUMN previous_stock TYPE NUMERIC(18,6),
    ALTER COLUMN resulting_stock TYPE NUMERIC(18,6);

ALTER TABLE inventory_reservations
    ALTER COLUMN quantity_base TYPE NUMERIC(18,6),
    ADD COLUMN unit_cost_snapshot NUMERIC(18,6);

ALTER TABLE order_item_consumptions
    ALTER COLUMN quantity_base TYPE NUMERIC(18,6),
    ALTER COLUMN unit_cost_snapshot DROP NOT NULL,
    ALTER COLUMN unit_cost_snapshot DROP DEFAULT,
    ALTER COLUMN unit_cost_snapshot TYPE NUMERIC(18,6);

CREATE TABLE order_item_ingredient_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    order_item_id UUID NOT NULL,
    master_ingredient_id UUID NOT NULL,
    ingredient_name_snapshot VARCHAR(180) NOT NULL,
    quantity_base_per_sale_unit NUMERIC(18,6) NOT NULL,
    unit_cost_snapshot NUMERIC(18,6),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_oiir_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_oiir_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_oiir_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_oiir_ingredient
        FOREIGN KEY (master_ingredient_id) REFERENCES master_ingredients(id),
    CONSTRAINT uq_oiir_item_ingredient
        UNIQUE (order_item_id, master_ingredient_id),
    CONSTRAINT chk_oiir_quantity
        CHECK (quantity_base_per_sale_unit > 0)
);

CREATE INDEX idx_oiir_order
    ON order_item_ingredient_requirements (restaurant_id, order_id);

CREATE INDEX idx_oiir_ingredient
    ON order_item_ingredient_requirements (restaurant_id, master_ingredient_id);
