/* Enable UUID extension (Required for gen_random_uuid() in some PG versions, useful utility) */
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

/* Enums */
CREATE TYPE operation_mode_enum AS ENUM ('PREPAID', 'POSTPAID');
CREATE TYPE billing_cycle_enum AS ENUM ('MONTHLY', 'YEARLY');

/* =========================================================================
   1. subscription_plans
   ========================================================================= */
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    billing_cycle billing_cycle_enum NOT NULL,
    description TEXT,
    currency VARCHAR(3) DEFAULT 'USD' NOT NULL,
    features JSONB NOT NULL DEFAULT '{}', -- Contains all feature flags and limits

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID, /* FK added later */
    updated_by UUID, /* FK added later */
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID  /* FK added later */
);

/* =========================================================================
   2. users
   ========================================================================= */
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    second_name VARCHAR(100),
    first_lastname VARCHAR(100) NOT NULL,
    second_lastname VARCHAR(100),
    phone_number VARCHAR(50) UNIQUE, /* Added UNIQUE constraint */
    username VARCHAR(100) UNIQUE,
    
    owner_id UUID, /* Self-Reference FK */
    subscription_plan_id UUID, /* FK to subscription_plans */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   3. restaurants
   ========================================================================= */
CREATE TABLE restaurants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    address TEXT,
    email VARCHAR(255), /* Added field */
    phone_number VARCHAR(50), /* Added field */
    operation_mode operation_mode_enum NOT NULL, /* PREPAID / POSTPAID */
    settings JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    owner_id UUID NOT NULL, /* FK to users */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   4. roles
   ========================================================================= */
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    permissions JSONB NOT NULL DEFAULT '{}',
    restaurant_id UUID, /* Optional: null = global role? or system role? */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   5. user_restaurant_roles
   ========================================================================= */
CREATE TABLE user_restaurant_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    restaurant_id UUID NOT NULL,
    role_id UUID NOT NULL,

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   6. taxes
   ========================================================================= */
CREATE TABLE taxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    percentage NUMERIC(10,2) NOT NULL,
    owner_id UUID NOT NULL, /* FK to Owners */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   7. restaurant_taxes
   ========================================================================= */
CREATE TABLE restaurant_taxes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    tax_id UUID NOT NULL,

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   CONSTRAINTS & FOREIGN KEYS
   ========================================================================= */

/* Subscription Plans FKs (Audit) */
ALTER TABLE subscription_plans ADD CONSTRAINT fk_sub_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE subscription_plans ADD CONSTRAINT fk_sub_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE subscription_plans ADD CONSTRAINT fk_sub_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id);

/* Users FKs */
ALTER TABLE users ADD CONSTRAINT fk_users_owner FOREIGN KEY (owner_id) REFERENCES users(id);
ALTER TABLE users ADD CONSTRAINT fk_users_plan FOREIGN KEY (subscription_plan_id) REFERENCES subscription_plans(id);
ALTER TABLE users ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE users ADD CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE users ADD CONSTRAINT fk_users_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id);

/* Restaurants FKs */
ALTER TABLE restaurants ADD CONSTRAINT fk_rest_owner FOREIGN KEY (owner_id) REFERENCES users(id);
ALTER TABLE restaurants ADD CONSTRAINT fk_rest_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE restaurants ADD CONSTRAINT fk_rest_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE restaurants ADD CONSTRAINT fk_rest_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id);

/* Roles FKs */
ALTER TABLE roles ADD CONSTRAINT fk_roles_rest FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
ALTER TABLE roles ADD CONSTRAINT fk_roles_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE roles ADD CONSTRAINT fk_roles_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE roles ADD CONSTRAINT fk_roles_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id);

/* User Restaurant Roles FKs */
ALTER TABLE user_restaurant_roles ADD CONSTRAINT fk_urr_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_restaurant_roles ADD CONSTRAINT fk_urr_rest FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
ALTER TABLE user_restaurant_roles ADD CONSTRAINT fk_urr_role FOREIGN KEY (role_id) REFERENCES roles(id);
ALTER TABLE user_restaurant_roles ADD CONSTRAINT fk_urr_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE user_restaurant_roles ADD CONSTRAINT fk_urr_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE user_restaurant_roles ADD CONSTRAINT fk_urr_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id);

/* Taxes FKs */
ALTER TABLE taxes ADD CONSTRAINT fk_taxes_owner FOREIGN KEY (owner_id) REFERENCES users(id);
ALTER TABLE taxes ADD CONSTRAINT fk_taxes_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE taxes ADD CONSTRAINT fk_taxes_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE taxes ADD CONSTRAINT fk_taxes_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id);

/* Restaurant Taxes FKs */
ALTER TABLE restaurant_taxes ADD CONSTRAINT fk_rt_rest FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
ALTER TABLE restaurant_taxes ADD CONSTRAINT fk_rt_tax FOREIGN KEY (tax_id) REFERENCES taxes(id);
ALTER TABLE restaurant_taxes ADD CONSTRAINT fk_rt_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE restaurant_taxes ADD CONSTRAINT fk_rt_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE restaurant_taxes ADD CONSTRAINT fk_rt_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id);

/* =========================================================================
   8. Seed Data (Initial Plans)
   ========================================================================= */
INSERT INTO subscription_plans (name, description, price, currency, billing_cycle, features) VALUES
('Standard', 'Essential tools for small restaurants', 29.99, 'USD', 'MONTHLY', '{"maxRestaurants": 1, "maxEmployees": 5, "advancedReporting": false, "prioritySupport": false, "multiUserAccess": false}'),
('Pro', 'Advanced features for growing businesses', 79.99, 'USD', 'MONTHLY', '{"maxRestaurants": 3, "maxEmployees": 20, "advancedReporting": true, "prioritySupport": false, "multiUserAccess": true}'),
('Ultimate', 'Complete solution for large enterprises', 199.99, 'USD', 'MONTHLY', '{"maxRestaurants": 10, "maxEmployees": 100, "advancedReporting": true, "prioritySupport": true, "multiUserAccess": true}');

/* =========================================================================
   9. Seed Data (System Roles)
   ========================================================================= */
/*
    Role Definitions & Intended Scopes:
    
    1. Super Admin: Global System (All Permissions)
    2. Owner: Tenant (All Restaurant Management)
    3. Branch Manager (Administrador): Operations, Inventory, Staff
    4. Chef: Kitchen Management, Recipes
    5. Cook (Cocinero): KDS Status
    6. Cashier (Cajero): Payments, Cash
    7. Waiter (Mesero): Orders, Tables
*/
INSERT INTO roles (name, permissions, restaurant_id) VALUES
('Owner', '{"INVENTORY": ["CREATE", "EDIT", "DELETE"]}', NULL);
-- ('Branch Manager', '{"operations": ["manage"], "inventory": ["manage"], "tables": ["edit"], "orders": ["void"]}', NULL),
-- ('Chef', '{"kds": ["view"], "recipes": ["manage"], "inventory": ["view_alerts"]}', NULL),
-- ('Cook', '{"kds": ["view", "update_status"]}', NULL),
-- ('Cashier', '{"cash": ["open", "close"], "payments": ["process"]}', NULL),
-- ('Waiter', '{"orders": ["create", "update"], "tables": ["assign_guests"], "kitchen": ["comment"]}', NULL);
