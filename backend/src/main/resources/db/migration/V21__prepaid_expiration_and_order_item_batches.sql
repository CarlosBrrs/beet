ALTER TABLE orders
    ADD COLUMN payment_expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN payment_expired_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN expiration_processed_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_orders_prepaid_expiration
    ON orders (payment_expires_at, id)
    WHERE order_status = 'AWAITING_PAYMENT'
      AND expiration_processed_at IS NULL;

ALTER TABLE order_item_cancellations
    ADD COLUMN system_generated BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE order_item_cancellations
    ALTER COLUMN created_by DROP NOT NULL;

ALTER TABLE order_item_cancellations
    ADD CONSTRAINT chk_oic_created_by_origin
        CHECK (
            (system_generated = TRUE AND created_by IS NULL)
            OR
            (system_generated = FALSE AND created_by IS NOT NULL)
        );

UPDATE restaurants
SET settings = jsonb_set(
    COALESCE(settings, '{}'::jsonb),
    '{prepaidOrderExpirationMinutes}',
    '30'::jsonb,
    TRUE
)
WHERE settings IS NULL
   OR NOT settings ? 'prepaidOrderExpirationMinutes';
