CREATE TYPE user_account_status AS ENUM ('ACTIVE', 'SUSPENDED');
CREATE TYPE staff_assignment_status AS ENUM ('ACTIVE', 'SUSPENDED', 'REMOVED');

CREATE OR REPLACE FUNCTION user_full_name(
    first_name TEXT,
    second_name TEXT,
    first_lastname TEXT,
    second_lastname TEXT
) RETURNS TEXT AS $$
    SELECT TRIM(CONCAT_WS(' ', first_name, second_name, first_lastname, second_lastname));
$$ LANGUAGE SQL IMMUTABLE;

CREATE OR REPLACE FUNCTION invitation_status(
    accepted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ
) RETURNS TEXT AS $$
    SELECT CASE
        WHEN accepted_at IS NOT NULL THEN 'ACCEPTED'
        WHEN revoked_at IS NOT NULL THEN 'REVOKED'
        WHEN expires_at <= NOW() THEN 'EXPIRED'
        ELSE 'PENDING'
    END;
$$ LANGUAGE SQL STABLE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS account_status user_account_status NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

ALTER TABLE user_restaurant_roles
    ADD COLUMN IF NOT EXISTS assignment_status staff_assignment_status NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS preset_key VARCHAR(40),
    ADD COLUMN IF NOT EXISTS is_assignable BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users SET account_status = 'ACTIVE' WHERE account_status IS NULL;
UPDATE user_restaurant_roles SET assignment_status = 'ACTIVE' WHERE assignment_status IS NULL;

UPDATE roles
SET preset_key = UPPER(REPLACE(name, ' ', '_'))
WHERE preset_key IS NULL
  AND restaurant_id IS NULL
  AND deleted_at IS NULL;

UPDATE roles
SET name = 'Owner',
    preset_key = 'OWNER',
    permissions = '{"ALL":["ALL"]}'::jsonb,
    is_active = TRUE,
    is_assignable = FALSE
WHERE restaurant_id IS NULL
  AND LOWER(name) = 'owner'
  AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_restaurant_roles_user_restaurant
    ON user_restaurant_roles (user_id, restaurant_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_global_preset
    ON roles (preset_key)
    WHERE restaurant_id IS NULL AND preset_key IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_roles_global_name
    ON roles (LOWER(name))
    WHERE restaurant_id IS NULL AND deleted_at IS NULL;

WITH system_roles(name, preset_key, is_assignable, permissions) AS (
    VALUES
    ('Owner', 'OWNER', FALSE,
     '{"ALL":["ALL"]}'::jsonb),
    ('Manager', 'MANAGER', TRUE,
     '{"STAFF":["MANAGE"],"RESTAURANTS":["VIEW","EDIT"],"TABLES":["MANAGE"],"ORDERS":["MANAGE"],"KDS":["MANAGE"],"CASH":["MANAGE"],"PAYMENTS":["MANAGE"],"INVENTORY":["MANAGE"],"INVOICES":["MANAGE"],"MENUS":["MANAGE"],"PRODUCTS":["MANAGE"],"PREPARATIONS":["MANAGE"],"TEMPLATES":["MANAGE"],"FINANCE":["VIEW"]}'::jsonb),
    ('Cashier', 'CASHIER', TRUE,
     '{"TABLES":["VIEW"],"ORDERS":["VIEW","CREATE","EDIT"],"PAYMENTS":["VIEW","PROCESS"],"CASH":["VIEW","OPEN","CLOSE","PROCESS"]}'::jsonb),
    ('Waiter', 'WAITER', TRUE,
     '{"TABLES":["VIEW"],"ORDERS":["VIEW","CREATE","EDIT"],"KDS":["VIEW"]}'::jsonb),
    ('Kitchen', 'KITCHEN', TRUE,
     '{"KDS":["VIEW","UPDATE_STATUS"],"ORDERS":["VIEW"]}'::jsonb)
)
INSERT INTO roles (name, permissions, restaurant_id, preset_key, is_active, is_assignable)
SELECT sr.name, sr.permissions, NULL, sr.preset_key, TRUE, sr.is_assignable
FROM system_roles sr
WHERE NOT EXISTS (
    SELECT 1
    FROM roles existing
    WHERE existing.restaurant_id IS NULL
      AND existing.preset_key = sr.preset_key
      AND existing.deleted_at IS NULL
);

WITH system_roles(name, preset_key, is_assignable, permissions) AS (
    VALUES
    ('Owner', 'OWNER', FALSE,
     '{"ALL":["ALL"]}'::jsonb),
    ('Manager', 'MANAGER', TRUE,
     '{"STAFF":["MANAGE"],"RESTAURANTS":["VIEW","EDIT"],"TABLES":["MANAGE"],"ORDERS":["MANAGE"],"KDS":["MANAGE"],"CASH":["MANAGE"],"PAYMENTS":["MANAGE"],"INVENTORY":["MANAGE"],"INVOICES":["MANAGE"],"MENUS":["MANAGE"],"PRODUCTS":["MANAGE"],"PREPARATIONS":["MANAGE"],"TEMPLATES":["MANAGE"],"FINANCE":["VIEW"]}'::jsonb),
    ('Cashier', 'CASHIER', TRUE,
     '{"TABLES":["VIEW"],"ORDERS":["VIEW","CREATE","EDIT"],"PAYMENTS":["VIEW","PROCESS"],"CASH":["VIEW","OPEN","CLOSE","PROCESS"]}'::jsonb),
    ('Waiter', 'WAITER', TRUE,
     '{"TABLES":["VIEW"],"ORDERS":["VIEW","CREATE","EDIT"],"KDS":["VIEW"]}'::jsonb),
    ('Kitchen', 'KITCHEN', TRUE,
     '{"KDS":["VIEW","UPDATE_STATUS"],"ORDERS":["VIEW"]}'::jsonb)
)
UPDATE roles r
SET name = sr.name,
    permissions = sr.permissions,
    is_active = TRUE,
    is_assignable = sr.is_assignable,
    updated_at = NOW()
FROM system_roles sr
WHERE r.restaurant_id IS NULL
  AND r.preset_key = sr.preset_key
  AND r.deleted_at IS NULL;

CREATE TABLE staff_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    restaurant_id UUID NOT NULL,
    role_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    accepted_by UUID,
    revoked_at TIMESTAMPTZ,
    revoked_by UUID,
    revoked_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID,
    CONSTRAINT fk_staff_inv_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_staff_inv_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
    CONSTRAINT fk_staff_inv_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_staff_inv_accepted_by FOREIGN KEY (accepted_by) REFERENCES users(id),
    CONSTRAINT fk_staff_inv_revoked_by FOREIGN KEY (revoked_by) REFERENCES users(id),
    CONSTRAINT fk_staff_inv_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_staff_inv_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT fk_staff_inv_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_inv_pending_email_restaurant
    ON staff_invitations (restaurant_id, LOWER(email))
    WHERE accepted_at IS NULL AND revoked_at IS NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_staff_inv_owner_restaurant
    ON staff_invitations (owner_id, restaurant_id, expires_at);

CREATE OR REPLACE FUNCTION ensure_restaurant_role_scope()
RETURNS trigger AS $$
DECLARE
    role_preset TEXT;
    role_active BOOLEAN;
    role_assignable BOOLEAN;
    role_deleted TIMESTAMPTZ;
    role_restaurant_id UUID;
BEGIN
    SELECT preset_key, is_active, is_assignable, deleted_at, restaurant_id
    INTO role_preset, role_active, role_assignable, role_deleted, role_restaurant_id
    FROM roles
    WHERE id = NEW.role_id;

    IF role_preset IS NULL OR role_deleted IS NOT NULL OR role_active IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'Role % is not active', NEW.role_id;
    END IF;

    IF role_restaurant_id IS NOT NULL THEN
        RAISE EXCEPTION 'Role % must be global', NEW.role_id;
    END IF;

    IF role_preset = 'OWNER' THEN
        RETURN NEW;
    END IF;

    IF role_assignable IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'Role % is not assignable', NEW.role_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_user_restaurant_roles_scope ON user_restaurant_roles;
CREATE TRIGGER trg_user_restaurant_roles_scope
BEFORE INSERT OR UPDATE OF role_id, restaurant_id ON user_restaurant_roles
FOR EACH ROW EXECUTE FUNCTION ensure_restaurant_role_scope();

CREATE OR REPLACE FUNCTION ensure_staff_invitation_role_scope()
RETURNS trigger AS $$
DECLARE
    role_active BOOLEAN;
    role_assignable BOOLEAN;
    role_deleted TIMESTAMPTZ;
    role_restaurant_id UUID;
BEGIN
    SELECT is_active, is_assignable, deleted_at, restaurant_id
    INTO role_active, role_assignable, role_deleted, role_restaurant_id
    FROM roles
    WHERE id = NEW.role_id;

    IF role_deleted IS NOT NULL
       OR role_restaurant_id IS NOT NULL
       OR role_active IS DISTINCT FROM TRUE
       OR role_assignable IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'Invitation role % is not assignable', NEW.role_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_staff_invitation_role_scope ON staff_invitations;
CREATE TRIGGER trg_staff_invitation_role_scope
BEFORE INSERT OR UPDATE OF role_id, restaurant_id ON staff_invitations
FOR EACH ROW EXECUTE FUNCTION ensure_staff_invitation_role_scope();
