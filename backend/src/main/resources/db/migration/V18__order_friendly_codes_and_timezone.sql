/* =========================================================================
   V18 — Order friendly codes and operational business date
   ========================================================================= */

CREATE OR REPLACE FUNCTION beet_order_public_code(value BIGINT)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    alphabet CONSTANT TEXT := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    alphabet_size CONSTANT INTEGER := length(alphabet);
    n BIGINT := GREATEST(value, 0);
    code TEXT := '';
    i INTEGER;
BEGIN
    FOR i IN 1..6 LOOP
        code := substr(alphabet, ((n % alphabet_size) + 1)::INTEGER, 1) || code;
        n := FLOOR(n::NUMERIC / alphabet_size)::BIGINT;
    END LOOP;
    RETURN code;
END;
$$;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS business_date DATE,
    ADD COLUMN IF NOT EXISTS daily_sequence INTEGER,
    ADD COLUMN IF NOT EXISTS public_code VARCHAR(16);

WITH ordered AS (
    SELECT
        id,
        restaurant_id,
        (created_at AT TIME ZONE 'America/Bogota')::DATE AS computed_business_date,
        ROW_NUMBER() OVER (
            PARTITION BY restaurant_id, (created_at AT TIME ZONE 'America/Bogota')::DATE
            ORDER BY created_at ASC, id ASC
        ) AS computed_sequence,
        ROW_NUMBER() OVER (
            PARTITION BY restaurant_id
            ORDER BY created_at ASC, id ASC
        ) AS computed_public_seed
    FROM orders
)
UPDATE orders o
   SET business_date = COALESCE(o.business_date, ordered.computed_business_date),
       daily_sequence = COALESCE(o.daily_sequence, ordered.computed_sequence),
       public_code = COALESCE(o.public_code, beet_order_public_code(ordered.computed_public_seed))
  FROM ordered
 WHERE o.id = ordered.id;

CREATE TABLE IF NOT EXISTS order_daily_sequences (
    restaurant_id UUID NOT NULL,
    business_date DATE NOT NULL,
    last_sequence INTEGER NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    PRIMARY KEY (restaurant_id, business_date),
    CONSTRAINT fk_order_daily_sequences_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT chk_order_daily_sequences_last_sequence
        CHECK (last_sequence > 0)
);

INSERT INTO order_daily_sequences (restaurant_id, business_date, last_sequence)
SELECT restaurant_id, business_date, MAX(daily_sequence)
  FROM orders
 GROUP BY restaurant_id, business_date
ON CONFLICT (restaurant_id, business_date)
DO UPDATE SET last_sequence = GREATEST(order_daily_sequences.last_sequence, EXCLUDED.last_sequence),
              updated_at = NOW();

ALTER TABLE orders
    ALTER COLUMN business_date SET NOT NULL,
    ALTER COLUMN daily_sequence SET NOT NULL,
    ALTER COLUMN public_code SET NOT NULL;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_daily_sequence CHECK (daily_sequence > 0);

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_daily_sequence
    ON orders (restaurant_id, business_date, daily_sequence);

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_public_code
    ON orders (restaurant_id, public_code);

DROP FUNCTION beet_order_public_code(BIGINT);
