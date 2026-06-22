/* =========================================================================
   V22 - Business days, cash movements, reconciliation and payment defaults.
   ========================================================================= */

CREATE TYPE business_day_status AS ENUM ('OPEN', 'CLOSED');
CREATE TYPE business_day_event_type AS ENUM ('OPENED', 'CLOSED', 'REOPENED');
CREATE TYPE cash_movement_direction AS ENUM ('IN', 'OUT');
CREATE TYPE cash_movement_reason AS ENUM (
    'CHANGE_FUND',
    'SAFE_DROP',
    'PETTY_EXPENSE',
    'CORRECTION',
    'OTHER'
);
CREATE TYPE cash_movement_status AS ENUM ('RECORDED', 'VOIDED');

CREATE TABLE restaurant_business_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurants(id),
    business_date DATE NOT NULL,
    time_zone_snapshot VARCHAR(80) NOT NULL,
    status business_day_status NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    opened_by UUID NOT NULL REFERENCES users(id),
    closed_at TIMESTAMP WITH TIME ZONE,
    closed_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_business_day_date UNIQUE (restaurant_id, business_date),
    CONSTRAINT uq_business_day_id_restaurant UNIQUE (id, restaurant_id),
    CONSTRAINT chk_business_day_state CHECK (
        (status = 'OPEN' AND closed_at IS NULL AND closed_by IS NULL)
        OR
        (status = 'CLOSED' AND closed_at IS NOT NULL AND closed_by IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_business_day_open
    ON restaurant_business_days (restaurant_id)
    WHERE status = 'OPEN';

CREATE TABLE business_day_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurants(id),
    business_day_id UUID NOT NULL REFERENCES restaurant_business_days(id),
    event_type business_day_event_type NOT NULL,
    reason TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    occurred_by UUID NOT NULL REFERENCES users(id),
    CONSTRAINT chk_business_day_event_reason CHECK (
        event_type <> 'REOPENED' OR NULLIF(BTRIM(reason), '') IS NOT NULL
    )
);

CREATE TABLE business_day_closures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurants(id),
    business_day_id UUID NOT NULL REFERENCES restaurant_business_days(id),
    closure_sequence INTEGER NOT NULL,
    payments_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    tips_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    refunds_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    cash_in_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    cash_out_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    expected_cash_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    counted_cash_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    difference_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    notes TEXT,
    closed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    closed_by UUID NOT NULL REFERENCES users(id),
    CONSTRAINT uq_business_day_closure_sequence UNIQUE (business_day_id, closure_sequence)
);

CREATE TABLE business_day_payment_totals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    closure_id UUID NOT NULL REFERENCES business_day_closures(id) ON DELETE CASCADE,
    payment_method_id UUID REFERENCES payment_methods(id),
    method_code_snapshot VARCHAR(80) NOT NULL,
    method_name_snapshot VARCHAR(120) NOT NULL,
    method_type_snapshot payment_method_type NOT NULL,
    payment_count INTEGER NOT NULL DEFAULT 0,
    payment_amount NUMERIC(15,4) NOT NULL DEFAULT 0,
    tip_amount NUMERIC(15,4) NOT NULL DEFAULT 0,
    refund_amount NUMERIC(15,4) NOT NULL DEFAULT 0,
    net_amount NUMERIC(15,4) NOT NULL DEFAULT 0,
    CONSTRAINT uq_business_day_payment_method UNIQUE (closure_id, method_code_snapshot)
);

ALTER TABLE business_day_events
    ADD CONSTRAINT fk_business_day_events_tenant
    FOREIGN KEY (business_day_id, restaurant_id)
    REFERENCES restaurant_business_days(id, restaurant_id);

ALTER TABLE business_day_closures
    ADD CONSTRAINT fk_business_day_closures_tenant
    FOREIGN KEY (business_day_id, restaurant_id)
    REFERENCES restaurant_business_days(id, restaurant_id);

ALTER TABLE cash_sessions
    ADD COLUMN business_day_id UUID REFERENCES restaurant_business_days(id);

ALTER TABLE orders
    ADD COLUMN business_day_id UUID REFERENCES restaurant_business_days(id);

ALTER TABLE cash_sessions
    ADD CONSTRAINT fk_cash_sessions_business_day_tenant
    FOREIGN KEY (business_day_id, restaurant_id)
    REFERENCES restaurant_business_days(id, restaurant_id);

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_business_day_tenant
    FOREIGN KEY (business_day_id, restaurant_id)
    REFERENCES restaurant_business_days(id, restaurant_id);

INSERT INTO restaurant_business_days (
    restaurant_id,
    business_date,
    time_zone_snapshot,
    status,
    opened_by
)
SELECT
    r.id,
    (NOW() AT TIME ZONE COALESCE(NULLIF(r.settings ->> 'timeZone', ''), 'America/Bogota'))::date,
    COALESCE(NULLIF(r.settings ->> 'timeZone', ''), 'America/Bogota'),
    'OPEN',
    r.owner_id
FROM restaurants r
WHERE EXISTS (
    SELECT 1
    FROM cash_sessions cs
    WHERE cs.restaurant_id = r.id AND cs.status = 'OPEN'
)
OR EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.restaurant_id = r.id
      AND o.order_status IN ('DRAFT', 'AWAITING_PAYMENT', 'OPEN')
)
ON CONFLICT (restaurant_id, business_date) DO NOTHING;

UPDATE cash_sessions cs
SET business_day_id = bd.id
FROM restaurant_business_days bd
WHERE cs.restaurant_id = bd.restaurant_id
  AND cs.status = 'OPEN'
  AND bd.status = 'OPEN'
  AND cs.business_day_id IS NULL;

UPDATE orders o
SET business_day_id = bd.id
FROM restaurant_business_days bd
WHERE o.restaurant_id = bd.restaurant_id
  AND o.order_status IN ('DRAFT', 'AWAITING_PAYMENT', 'OPEN')
  AND bd.status = 'OPEN'
  AND o.business_day_id IS NULL;

INSERT INTO business_day_events (
    restaurant_id,
    business_day_id,
    event_type,
    reason,
    occurred_by
)
SELECT
    bd.restaurant_id,
    bd.id,
    'OPENED',
    'Migrated active commercial operations into V22 business day',
    bd.opened_by
FROM restaurant_business_days bd
WHERE NOT EXISTS (
    SELECT 1 FROM business_day_events event WHERE event.business_day_id = bd.id
);

CREATE INDEX idx_cash_sessions_business_day
    ON cash_sessions (business_day_id, status);
CREATE INDEX idx_orders_business_day
    ON orders (business_day_id, order_status);

CREATE TABLE cash_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurants(id),
    business_day_id UUID NOT NULL REFERENCES restaurant_business_days(id),
    cash_session_id UUID NOT NULL REFERENCES cash_sessions(id),
    direction cash_movement_direction NOT NULL,
    reason cash_movement_reason NOT NULL,
    amount NUMERIC(15,4) NOT NULL,
    status cash_movement_status NOT NULL DEFAULT 'RECORDED',
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL REFERENCES users(id),
    created_device_id UUID NOT NULL,
    voided_at TIMESTAMP WITH TIME ZONE,
    voided_by UUID REFERENCES users(id),
    voided_device_id UUID,
    void_reason TEXT,
    CONSTRAINT chk_cash_movement_amount CHECK (amount > 0),
    CONSTRAINT chk_cash_movement_note CHECK (
        reason NOT IN ('OTHER', 'CORRECTION')
        OR NULLIF(BTRIM(notes), '') IS NOT NULL
    ),
    CONSTRAINT chk_cash_movement_void CHECK (
        (status = 'RECORDED'
            AND voided_at IS NULL
            AND voided_by IS NULL
            AND voided_device_id IS NULL
            AND void_reason IS NULL)
        OR
        (status = 'VOIDED'
            AND voided_at IS NOT NULL
            AND voided_by IS NOT NULL
            AND voided_device_id IS NOT NULL
            AND NULLIF(BTRIM(void_reason), '') IS NOT NULL)
    )
);

CREATE INDEX idx_cash_movements_session
    ON cash_movements (cash_session_id, created_at DESC);
CREATE INDEX idx_cash_movements_business_day
    ON cash_movements (business_day_id, status);

CREATE TABLE cash_session_closures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL REFERENCES restaurants(id),
    business_day_id UUID NOT NULL REFERENCES restaurant_business_days(id),
    cash_session_id UUID NOT NULL REFERENCES cash_sessions(id),
    opening_amount NUMERIC(15,4) NOT NULL,
    payments_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    tips_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    refunds_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    cash_in_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    cash_out_total NUMERIC(15,4) NOT NULL DEFAULT 0,
    expected_cash NUMERIC(15,4) NOT NULL,
    counted_cash NUMERIC(15,4) NOT NULL,
    difference_amount NUMERIC(15,4) NOT NULL,
    difference_reason TEXT,
    notes TEXT,
    closed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    closed_by UUID NOT NULL REFERENCES users(id),
    closed_device_id UUID NOT NULL,
    CONSTRAINT uq_cash_session_closure UNIQUE (cash_session_id),
    CONSTRAINT chk_cash_session_closure_amounts CHECK (
        opening_amount >= 0
        AND payments_total >= 0
        AND tips_total >= 0
        AND refunds_total >= 0
        AND cash_in_total >= 0
        AND cash_out_total >= 0
        AND counted_cash >= 0
    ),
    CONSTRAINT chk_cash_session_difference_reason CHECK (
        difference_amount = 0
        OR NULLIF(BTRIM(difference_reason), '') IS NOT NULL
    )
);

CREATE TABLE cash_session_payment_totals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    closure_id UUID NOT NULL REFERENCES cash_session_closures(id) ON DELETE CASCADE,
    payment_method_id UUID REFERENCES payment_methods(id),
    method_code_snapshot VARCHAR(80) NOT NULL,
    method_name_snapshot VARCHAR(120) NOT NULL,
    method_type_snapshot payment_method_type NOT NULL,
    payment_count INTEGER NOT NULL DEFAULT 0,
    payment_amount NUMERIC(15,4) NOT NULL DEFAULT 0,
    tip_amount NUMERIC(15,4) NOT NULL DEFAULT 0,
    refund_amount NUMERIC(15,4) NOT NULL DEFAULT 0,
    net_amount NUMERIC(15,4) NOT NULL DEFAULT 0,
    CONSTRAINT uq_cash_session_payment_method UNIQUE (closure_id, method_code_snapshot)
);

ALTER TABLE cash_movements
    ADD CONSTRAINT fk_cash_movements_business_day_tenant
    FOREIGN KEY (business_day_id, restaurant_id)
    REFERENCES restaurant_business_days(id, restaurant_id);

ALTER TABLE cash_movements
    ADD CONSTRAINT fk_cash_movements_session_tenant
    FOREIGN KEY (cash_session_id, restaurant_id)
    REFERENCES cash_sessions(id, restaurant_id);

ALTER TABLE cash_session_closures
    ADD CONSTRAINT fk_cash_session_closures_business_day_tenant
    FOREIGN KEY (business_day_id, restaurant_id)
    REFERENCES restaurant_business_days(id, restaurant_id);

ALTER TABLE cash_session_closures
    ADD CONSTRAINT fk_cash_session_closures_session_tenant
    FOREIGN KEY (cash_session_id, restaurant_id)
    REFERENCES cash_sessions(id, restaurant_id);

UPDATE restaurants
SET settings = jsonb_set(
    COALESCE(settings, '{}'::jsonb),
    '{cashCountMode}',
    '"BLIND"'::jsonb,
    TRUE
)
WHERE COALESCE(settings ->> 'cashCountMode', '') = '';

INSERT INTO payment_methods (
    restaurant_id,
    code,
    name,
    type,
    is_active,
    requires_reference,
    sort_order,
    created_by,
    updated_by
)
SELECT
    r.id,
    defaults.code,
    defaults.name,
    defaults.type::payment_method_type,
    TRUE,
    defaults.requires_reference,
    defaults.sort_order,
    r.owner_id,
    r.owner_id
FROM restaurants r
CROSS JOIN (
    VALUES
        ('CASH', 'Efectivo', 'CASH', FALSE, 10),
        ('DEBIT_CARD', 'Tarjeta débito', 'DEBIT_CARD', TRUE, 20),
        ('CREDIT_CARD', 'Tarjeta crédito', 'CREDIT_CARD', TRUE, 30),
        ('NEQUI', 'Nequi', 'NEQUI', TRUE, 40),
        ('DAVIPLATA', 'Daviplata', 'DAVIPLATA', TRUE, 50)
) AS defaults(code, name, type, requires_reference, sort_order)
WHERE NOT EXISTS (
    SELECT 1
    FROM payment_methods pm
    WHERE pm.restaurant_id = r.id
      AND pm.code = defaults.code
);
