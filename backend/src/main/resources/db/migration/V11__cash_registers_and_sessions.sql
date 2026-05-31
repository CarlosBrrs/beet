/* =========================================================================
   V11 — Cash registers and sessions (MVP)
   Purpose: register devices and track open/close sessions per restaurant.
   ========================================================================= */

/* =========================================================================
   Enums
   ========================================================================= */
CREATE TYPE cash_session_status AS ENUM (
    'OPEN',
    'CLOSED'
);

/* =========================================================================
   1. cash_registers
   ========================================================================= */
CREATE TABLE cash_registers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,

    name VARCHAR(120) NOT NULL,
    device_id UUID, /* Optional binding to a device */
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL
);

/* =========================================================================
   2. cash_sessions
   ========================================================================= */
CREATE TABLE cash_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    cash_register_id UUID NOT NULL,

    status cash_session_status NOT NULL DEFAULT 'OPEN',

    opened_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    opened_by UUID NOT NULL,
    opened_device_id UUID NOT NULL,
    opening_amount NUMERIC(15,4) NOT NULL DEFAULT 0,

    closed_at TIMESTAMP WITH TIME ZONE,
    closed_by UUID,
    closed_device_id UUID,
    closing_amount NUMERIC(15,4),
    notes TEXT
);

/* =========================================================================
   3. orders (add cash_session_id)
   ========================================================================= */
ALTER TABLE orders ADD COLUMN cash_session_id UUID NOT NULL;

/* =========================================================================
   CONSTRAINTS & FOREIGN KEYS
   ========================================================================= */

/* cash_registers */
ALTER TABLE cash_registers ADD CONSTRAINT fk_cash_registers_restaurant
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
ALTER TABLE cash_registers ADD CONSTRAINT fk_cash_registers_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE cash_registers ADD CONSTRAINT fk_cash_registers_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id);

/* cash_sessions */
ALTER TABLE cash_sessions ADD CONSTRAINT fk_cash_sessions_restaurant
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
ALTER TABLE cash_sessions ADD CONSTRAINT fk_cash_sessions_register
    FOREIGN KEY (cash_register_id) REFERENCES cash_registers(id) ON DELETE RESTRICT;
ALTER TABLE cash_sessions ADD CONSTRAINT fk_cash_sessions_opened_by
    FOREIGN KEY (opened_by) REFERENCES users(id);
ALTER TABLE cash_sessions ADD CONSTRAINT fk_cash_sessions_closed_by
    FOREIGN KEY (closed_by) REFERENCES users(id);

/* orders */
ALTER TABLE orders ADD CONSTRAINT fk_orders_cash_session
    FOREIGN KEY (cash_session_id) REFERENCES cash_sessions(id);

/* =========================================================================
   Indexes & Uniqueness
   ========================================================================= */
CREATE UNIQUE INDEX uq_cash_registers_rest_name
    ON cash_registers (restaurant_id, LOWER(name));
CREATE UNIQUE INDEX uq_cash_registers_device
    ON cash_registers (restaurant_id, device_id)
    WHERE device_id IS NOT NULL;

/* One open session per register */
CREATE UNIQUE INDEX uq_cash_sessions_open_register
    ON cash_sessions (cash_register_id)
    WHERE status = 'OPEN';

/* One open session per device per restaurant */
CREATE UNIQUE INDEX uq_cash_sessions_open_device
    ON cash_sessions (restaurant_id, opened_device_id)
    WHERE status = 'OPEN';

CREATE INDEX idx_cash_sessions_rest_status
    ON cash_sessions (restaurant_id, status, opened_at DESC);
CREATE INDEX idx_cash_sessions_register
    ON cash_sessions (cash_register_id);

CREATE INDEX idx_orders_cash_session
    ON orders (cash_session_id);

ALTER TABLE cash_sessions ADD CONSTRAINT chk_cash_session_amounts
    CHECK (
        opening_amount >= 0
        AND (closing_amount IS NULL OR closing_amount >= 0)
    );

ALTER TABLE cash_sessions ADD CONSTRAINT chk_cash_session_close_state
    CHECK (
        (status = 'OPEN'
            AND closed_at IS NULL
            AND closed_by IS NULL
            AND closed_device_id IS NULL
            AND closing_amount IS NULL)
        OR
        (status = 'CLOSED'
            AND closed_at IS NOT NULL
            AND closed_by IS NOT NULL
            AND closed_device_id IS NOT NULL
            AND closing_amount IS NOT NULL)
    );
