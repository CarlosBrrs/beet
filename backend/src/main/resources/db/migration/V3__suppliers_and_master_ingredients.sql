/* =========================================================================
   Suppliers, Master Ingredients & Supplier Items
   ========================================================================= */

/* =========================================================================
   Enums
   ========================================================================= */
CREATE TYPE document_type_enum AS ENUM (
    'NIT',       -- Colombia
    'CC',        -- Cédula de Ciudadanía (CO)
    'CE',        -- Cédula de Extranjería (CO)
    'RUT',       -- Registro Único Tributario (CO/CL)
    'RFC',       -- Registro Federal de Contribuyentes (MX)
    'CURP',      -- Clave Única de Registro de Población (MX)
    'DNI',       -- Documento Nacional de Identidad (AR/PE)
    'CUIT',      -- Clave Única de Identificación Tributaria (AR)
    'RUC',       -- Registro Único de Contribuyentes (PE/EC)
    'PASSPORT'   -- International
);

/* =========================================================================
   1. countries
   ========================================================================= */
CREATE TABLE countries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code CHAR(2) NOT NULL UNIQUE,     /* ISO 3166-1 alpha-2 (e.g. CO, MX, US) */
    name VARCHAR(100) NOT NULL,
    phone_prefix VARCHAR(10) NOT NULL          /* International dialing code (e.g. +57, +1) */
);

/* =========================================================================
   2. document_types
   ========================================================================= */
CREATE TABLE document_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name document_type_enum NOT NULL,          /* Legal document identifier (NIT, CC, RFC, etc.) */
    country_id UUID NOT NULL,                  /* Country where this document type is valid */
    description VARCHAR(255),                  /* Human-readable explanation of the document */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

/* =========================================================================
   3. suppliers
   ========================================================================= */
CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,                    /* Tenant owner – suppliers are shared across their restaurants */
    document_type_id UUID NOT NULL,            /* Legal ID type (e.g. NIT, RFC) */
    document_number VARCHAR(50) NOT NULL,      /* Legal ID value (e.g. 900.123.456-7) */
    name VARCHAR(255) NOT NULL,                /* Commercial or legal name */
    contact_name VARCHAR(255),                 /* Primary contact person */
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   4. master_ingredients
   ========================================================================= */
CREATE TABLE master_ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,                    /* Tenant owner – ingredients are shared across their restaurants */
    name VARCHAR(255) NOT NULL,                /* Generic name (e.g. "Wheat Flour", "Tomato") */
    base_unit_id UUID NOT NULL,                /* Smallest unit for storage/costing (g, ml, pcs) */
    active_supplier_item_id UUID,              /* Currently selected supplier offering – determines cost. Nullable, FK added later (circular) */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   5. supplier_items
   ========================================================================= */
CREATE TABLE supplier_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    master_ingredient_id UUID NOT NULL,        /* The generic ingredient this offering belongs to */
    supplier_id UUID NOT NULL,                 /* Who sells it */
    brand_name VARCHAR(255),                   /* Commercial name (e.g. "Harina San Pablo 25kg") */
    purchase_unit_name VARCHAR(50) NOT NULL,    /* How it's bought: 'Sack', 'Box', 'Jug', etc. */
    conversion_factor NUMERIC(15, 6) NOT NULL,  /* How many base units (g/ml) per purchase unit */
    last_cost_base NUMERIC(15, 2) NOT NULL,     /* Cost per base unit = Price Paid / (Qty × Factor) */

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   CONSTRAINTS & FOREIGN KEYS
   ========================================================================= */

/* document_types */
ALTER TABLE document_types ADD CONSTRAINT fk_doctype_country         /* Each doc type belongs to one country */
    FOREIGN KEY (country_id) REFERENCES countries(id);
ALTER TABLE document_types ADD CONSTRAINT uq_doctype_name_country    /* No duplicate doc types within the same country */
    UNIQUE (name, country_id);

/* suppliers */
ALTER TABLE suppliers ADD CONSTRAINT fk_supplier_owner               /* Supplier belongs to a tenant owner */
    FOREIGN KEY (owner_id) REFERENCES users(id);
ALTER TABLE suppliers ADD CONSTRAINT fk_supplier_doctype             /* Supplier's legal identification type */
    FOREIGN KEY (document_type_id) REFERENCES document_types(id);
ALTER TABLE suppliers ADD CONSTRAINT fk_supplier_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE suppliers ADD CONSTRAINT fk_supplier_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE suppliers ADD CONSTRAINT fk_supplier_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES users(id);
ALTER TABLE suppliers ADD CONSTRAINT uq_supplier_owner_doc           /* Same owner can't register same doc twice */
    UNIQUE (owner_id, document_type_id, document_number);

/* master_ingredients */
ALTER TABLE master_ingredients ADD CONSTRAINT fk_mi_owner            /* Ingredient belongs to a tenant owner */
    FOREIGN KEY (owner_id) REFERENCES users(id);
ALTER TABLE master_ingredients ADD CONSTRAINT fk_mi_base_unit        /* Links to the base unit (g, ml, pcs) from units table */
    FOREIGN KEY (base_unit_id) REFERENCES units(id);
ALTER TABLE master_ingredients ADD CONSTRAINT fk_mi_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE master_ingredients ADD CONSTRAINT fk_mi_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE master_ingredients ADD CONSTRAINT fk_mi_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES users(id);

/* supplier_items */
ALTER TABLE supplier_items ADD CONSTRAINT fk_si_master_ingredient    /* Which ingredient this offering is for – cascades on delete */
    FOREIGN KEY (master_ingredient_id) REFERENCES master_ingredients(id) ON DELETE CASCADE;
ALTER TABLE supplier_items ADD CONSTRAINT fk_si_supplier             /* Which supplier sells it – cascades on delete */
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE;
ALTER TABLE supplier_items ADD CONSTRAINT fk_si_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE supplier_items ADD CONSTRAINT fk_si_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE supplier_items ADD CONSTRAINT fk_si_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES users(id);

/* Circular FK: the "winning" supplier item that determines the ingredient's current cost.
   Added last to avoid chicken-and-egg problem. SET NULL on delete so ingredient survives. */
ALTER TABLE master_ingredients ADD CONSTRAINT fk_mi_active_supplier_item
    FOREIGN KEY (active_supplier_item_id) REFERENCES supplier_items(id) ON DELETE SET NULL;

/* =========================================================================
   SEED DATA
   ========================================================================= */

/* Countries */
INSERT INTO countries (country_code, name, phone_prefix) VALUES
    ('CO', 'Colombia',      '+57'),
    ('MX', 'México',        '+52'),
    ('AR', 'Argentina',     '+54'),
    ('PE', 'Perú',          '+51'),
    ('EC', 'Ecuador',       '+593'),
    ('CL', 'Chile',         '+56'),
    ('US', 'United States', '+1');

/* Document Types (linked to countries) */
DO $$
DECLARE
    co_id UUID;
    mx_id UUID;
    ar_id UUID;
    pe_id UUID;
    ec_id UUID;
BEGIN
    SELECT id INTO co_id FROM countries WHERE country_code = 'CO';
    SELECT id INTO mx_id FROM countries WHERE country_code = 'MX';
    SELECT id INTO ar_id FROM countries WHERE country_code = 'AR';
    SELECT id INTO pe_id FROM countries WHERE country_code = 'PE';
    SELECT id INTO ec_id FROM countries WHERE country_code = 'EC';

    -- Colombia
    INSERT INTO document_types (name, country_id, description) VALUES
        ('NIT', co_id, 'Número de Identificación Tributaria'),
        ('CC',  co_id, 'Cédula de Ciudadanía'),
        ('CE',  co_id, 'Cédula de Extranjería');

    -- México
    INSERT INTO document_types (name, country_id, description) VALUES
        ('RFC',  mx_id, 'Registro Federal de Contribuyentes'),
        ('CURP', mx_id, 'Clave Única de Registro de Población');

    -- Argentina
    INSERT INTO document_types (name, country_id, description) VALUES
        ('DNI',  ar_id, 'Documento Nacional de Identidad'),
        ('CUIT', ar_id, 'Clave Única de Identificación Tributaria');

    -- Perú
    INSERT INTO document_types (name, country_id, description) VALUES
        ('DNI', pe_id, 'Documento Nacional de Identidad'),
        ('RUC', pe_id, 'Registro Único de Contribuyentes');

    -- Ecuador
    INSERT INTO document_types (name, country_id, description) VALUES
        ('RUC', ec_id, 'Registro Único de Contribuyentes');

    -- International (all countries)
    INSERT INTO document_types (name, country_id, description) VALUES
        ('PASSPORT', co_id, 'Pasaporte Internacional'),
        ('PASSPORT', mx_id, 'Pasaporte Internacional'),
        ('PASSPORT', ar_id, 'Pasaporte Internacional'),
        ('PASSPORT', pe_id, 'Pasaporte Internacional'),
        ('PASSPORT', ec_id, 'Pasaporte Internacional');
END $$;
