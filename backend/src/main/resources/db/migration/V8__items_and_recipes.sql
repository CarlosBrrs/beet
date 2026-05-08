/* =========================================================================
   V8 — Items & Recipes (Production BOM Engine)
   Layers 1 (PREPARATION) and 2 (SALEABLE_PRODUCT)
   ========================================================================= */

/* =========================================================================
   Enums
   ========================================================================= */
CREATE TYPE item_class AS ENUM (
    'PREPARATION',       -- Internal sub-recipe (not sold directly)
    'SALEABLE_PRODUCT'   -- Sellable product (flat or with recipe)
);

CREATE TYPE recipe_line_source AS ENUM (
    'INGREDIENT',   -- Points to a master_ingredient
    'PREPARATION'   -- Points to another item of class PREPARATION
);

/* =========================================================================
   1. items
   ========================================================================= */
CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,                   /* Tenant restaurant */
    class item_class NOT NULL,

    name VARCHAR(255) NOT NULL,
    description TEXT,

    /* ---- Inventory Tracking ------------------------------------------- */
    /* false = "Flat Product": sold without BOM; user defines theoretical_cost */
    /* true  = tracked: BOM required; system calculates theoretical_cost      */
    is_inventory_tracked BOOLEAN NOT NULL DEFAULT TRUE,

    /* ---- Yield (how much this recipe/prep produces) ------------------- */
    /* Stored in the unit the user specified; normalized at calculation time */
    /* Mode A — Weight/Volume: e.g. yield_qty=1.5, yield_unit=kg            */
    /* Mode B — Unit count:    e.g. yield_qty=20,  yield_unit=pcs           */
    yield_qty NUMERIC(15,4) NOT NULL DEFAULT 1,
    yield_unit_id UUID NOT NULL,

    /* ---- Pricing (SALEABLE_PRODUCT only) ------------------------------ */
    /* sale_price: set by user — the price charged to the customer          */
    /* theoretical_cost: calculated by system from BOM (tracked=true)       */
    /*                   OR entered manually by user (tracked=false)         */
    sale_price NUMERIC(15,2),
    theoretical_cost NUMERIC(15,4),

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID,

    /* Business rules */
    CONSTRAINT chk_flat_product_class
        CHECK (is_inventory_tracked = TRUE OR class = 'SALEABLE_PRODUCT'),

    CONSTRAINT chk_prep_no_price
        CHECK (class != 'PREPARATION' OR sale_price IS NULL)
);

/* =========================================================================
   2. recipe_lines  (Bill of Materials rows)
   ========================================================================= */
CREATE TABLE recipe_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_item_id UUID NOT NULL,                  /* The item this line belongs to */

    source recipe_line_source NOT NULL,

    /* Exactly one of these must be non-null (enforced by CHECK below) */
    master_ingredient_id UUID,                     /* Source = INGREDIENT */
    child_item_id UUID,                            /* Source = PREPARATION (class must be PREPARATION) */

    /* Quantity stored in the unit the user chose; normalised at calc time */
    quantity NUMERIC(15,4) NOT NULL,
    unit_id UUID NOT NULL,

    sort_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_recipe_line_source CHECK (
        (source = 'INGREDIENT'  AND master_ingredient_id IS NOT NULL AND child_item_id IS NULL) OR
        (source = 'PREPARATION' AND child_item_id IS NOT NULL        AND master_ingredient_id IS NULL)
    )
);

/* =========================================================================
   CONSTRAINTS & FOREIGN KEYS
   ========================================================================= */

/* items */
ALTER TABLE items ADD CONSTRAINT fk_item_restaurant
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE;
ALTER TABLE items ADD CONSTRAINT fk_item_yield_unit
    FOREIGN KEY (yield_unit_id) REFERENCES units(id);
ALTER TABLE items ADD CONSTRAINT fk_item_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE items ADD CONSTRAINT fk_item_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE items ADD CONSTRAINT fk_item_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES users(id);

CREATE UNIQUE INDEX uq_item_name_restaurant
    ON items (restaurant_id, LOWER(name));

/* recipe_lines */
ALTER TABLE recipe_lines ADD CONSTRAINT fk_rl_parent
    FOREIGN KEY (parent_item_id) REFERENCES items(id) ON DELETE CASCADE;
ALTER TABLE recipe_lines ADD CONSTRAINT fk_rl_ingredient
    FOREIGN KEY (master_ingredient_id) REFERENCES master_ingredients(id);
ALTER TABLE recipe_lines ADD CONSTRAINT fk_rl_child_item
    FOREIGN KEY (child_item_id) REFERENCES items(id);
ALTER TABLE recipe_lines ADD CONSTRAINT fk_rl_unit
    FOREIGN KEY (unit_id) REFERENCES units(id);

/* Performance indexes */
CREATE INDEX idx_items_restaurant_class ON items (restaurant_id, class);
CREATE INDEX idx_recipe_lines_parent ON recipe_lines (parent_item_id);
