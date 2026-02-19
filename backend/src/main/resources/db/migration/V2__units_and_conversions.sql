/* =========================================================================
   Units of Measure & Conversion System
   ========================================================================= */

/* Enum for unit categories */
CREATE TYPE unit_type AS ENUM ('MASS', 'VOLUME', 'UNIT');

/* =========================================================================
   1. units
   ========================================================================= */
CREATE TABLE units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    abbreviation VARCHAR(10) NOT NULL,
    type unit_type NOT NULL,
    is_base BOOLEAN DEFAULT FALSE,

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

/* =========================================================================
   2. unit_conversions
   ========================================================================= */
CREATE TABLE unit_conversions (
    from_unit_id UUID NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    to_unit_id UUID NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    factor NUMERIC(15, 6) NOT NULL,
    PRIMARY KEY (from_unit_id, to_unit_id)
);

/* =========================================================================
   3. Seed Data: Base Units
   ========================================================================= */
INSERT INTO units (name, abbreviation, type, is_base) VALUES
    ('Gram',       'g',   'MASS',   true),
    ('Milliliter', 'ml',  'VOLUME', true),
    ('Piece',      'pcs', 'UNIT',   true);

/* =========================================================================
   4. Seed Data: Derived Units + Conversions
   
   We use a DO block to capture generated UUIDs for the conversion inserts.
   ========================================================================= */
DO $$
DECLARE
    -- Base unit IDs (already inserted above)
    g_id   UUID;
    ml_id  UUID;

    -- Derived unit IDs
    kg_id      UUID := gen_random_uuid();
    lb_453_id  UUID := gen_random_uuid();
    lb_500_id  UUID := gen_random_uuid();
    oz_wt_id   UUID := gen_random_uuid();
    l_id       UUID := gen_random_uuid();
    fl_oz_id   UUID := gen_random_uuid();
    gal_id     UUID := gen_random_uuid();
BEGIN
    -- Fetch base unit IDs
    SELECT id INTO g_id  FROM units WHERE abbreviation = 'g';
    SELECT id INTO ml_id FROM units WHERE abbreviation = 'ml';

    -- Mass units
    INSERT INTO units (id, name, abbreviation, type) VALUES (kg_id,     'Kilogram',       'kg',    'MASS');
    INSERT INTO units (id, name, abbreviation, type) VALUES (lb_453_id, 'Pound (453g)',   'lb453', 'MASS');
    INSERT INTO units (id, name, abbreviation, type) VALUES (lb_500_id, 'Pound (500g)',   'lb500', 'MASS');
    INSERT INTO units (id, name, abbreviation, type) VALUES (oz_wt_id,  'Ounce (Weight)', 'oz',    'MASS');

    -- Volume units
    INSERT INTO units (id, name, abbreviation, type) VALUES (l_id,    'Liter',       'L',     'VOLUME');
    INSERT INTO units (id, name, abbreviation, type) VALUES (fl_oz_id,'Fluid Ounce', 'fl oz', 'VOLUME');
    INSERT INTO units (id, name, abbreviation, type) VALUES (gal_id,  'Gallon',      'gal',   'VOLUME');

    -- Mass conversions (to base unit: gram)
    INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor) VALUES (kg_id,     g_id, 1000);
    INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor) VALUES (lb_453_id, g_id, 453.592);
    INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor) VALUES (lb_500_id, g_id, 500);
    INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor) VALUES (oz_wt_id,  g_id, 28.3495);

    -- Volume conversions (to base unit: milliliter)
    INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor) VALUES (l_id,     ml_id, 1000);
    INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor) VALUES (fl_oz_id, ml_id, 29.5735);
    INSERT INTO unit_conversions (from_unit_id, to_unit_id, factor) VALUES (gal_id,   ml_id, 3785.41);
END $$;
