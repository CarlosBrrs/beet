/* Reporting remains query-based. These indexes support the first report providers. */

CREATE INDEX IF NOT EXISTS idx_orders_reports_business_date
    ON orders (restaurant_id, business_date, order_status);

CREATE INDEX IF NOT EXISTS idx_order_items_reports_line
    ON order_items (order_id, line_type, item_id, template_id);

CREATE INDEX IF NOT EXISTS idx_payments_reports_created
    ON payments (restaurant_id, created_at, status, payment_method_id);

CREATE INDEX IF NOT EXISTS idx_payment_refunds_reports_created
    ON payment_refunds (restaurant_id, created_at, status, payment_id);

CREATE INDEX IF NOT EXISTS idx_business_days_reports_date
    ON restaurant_business_days (restaurant_id, business_date DESC, status);

CREATE INDEX IF NOT EXISTS idx_business_day_events_reports_day
    ON business_day_events (business_day_id, occurred_at);

CREATE INDEX IF NOT EXISTS idx_business_day_closures_reports_day
    ON business_day_closures (business_day_id, closure_sequence DESC);

CREATE INDEX IF NOT EXISTS idx_consumptions_reports_created
    ON order_item_consumptions (restaurant_id, created_at, master_ingredient_id);

CREATE INDEX IF NOT EXISTS idx_inventory_transactions_reports_created
    ON inventory_transactions (ingredient_stock_id, created_at, reason);
