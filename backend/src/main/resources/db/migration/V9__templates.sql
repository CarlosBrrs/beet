/* =========================================================================
   V9 — Templates, Slots, Options & Submenu Nodes
   Layer 3 (TEMPLATE / Combo) + Menu-Item bridge
   ========================================================================= */

/* =========================================================================
   Enums
   ========================================================================= */
CREATE TYPE submenu_node_type AS ENUM (
    'PRODUCT',   -- Points to items (class = SALEABLE_PRODUCT)
    'TEMPLATE'   -- Points to a template
);

/* =========================================================================
   1. templates
   ========================================================================= */
CREATE TABLE templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,

    name VARCHAR(255) NOT NULL,
    description TEXT,

    /* base_price: the starting price of the combo shown to the customer.
       Final price = base_price + SUM of surcharges on chosen slot options. */
    base_price NUMERIC(15,2) NOT NULL DEFAULT 0,

    /* Audit */
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID
);

/* =========================================================================
   2. template_slots  (e.g. "Choose your protein", "Choose your side")
   ========================================================================= */
CREATE TABLE template_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,

    name VARCHAR(100) NOT NULL,

    /* min_selection = 0 → optional slot (not required)
       min_selection > 0 → required slot
       max_selection = upper bound of how many options can be chosen        */
    min_selection INT NOT NULL DEFAULT 1,
    max_selection INT NOT NULL DEFAULT 1,

    sort_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_slot_selection
        CHECK (min_selection >= 0 AND min_selection <= max_selection)
);

/* =========================================================================
   3. slot_options  (specific choices within a slot)
   Only SALEABLE_PRODUCT items can be options — never a PREPARATION.
   ========================================================================= */
CREATE TABLE slot_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id UUID NOT NULL,

    /* Must reference an item with class = 'SALEABLE_PRODUCT'.
       Enforced by application layer validation (cannot add DB CHECK for this). */
    item_id UUID NOT NULL,

    /* surcharge = extra cost added to base_price when this option is chosen.
       0 means no extra charge.                                               */
    surcharge NUMERIC(15,2) NOT NULL DEFAULT 0,

    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0
);

/* =========================================================================
   4. submenu_nodes  — bridge: Submenú → Carta Digital
   Products and Templates always belong to a Submenu.
   They cannot exist standalone (enforced at API endpoint level).
   ========================================================================= */
CREATE TABLE submenu_nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submenu_id UUID NOT NULL,

    node_type submenu_node_type NOT NULL,

    /* Only one FK is populated (the other is NULL), enforced by CHECK */
    item_id UUID,         /* Filled when node_type = 'PRODUCT'   */
    template_id UUID,     /* Filled when node_type = 'TEMPLATE'  */

    sort_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_submenu_node CHECK (
        (node_type = 'PRODUCT'   AND item_id IS NOT NULL     AND template_id IS NULL) OR
        (node_type = 'TEMPLATE'  AND template_id IS NOT NULL AND item_id IS NULL)
    )
);

/* =========================================================================
   CONSTRAINTS & FOREIGN KEYS
   ========================================================================= */

/* templates */
ALTER TABLE templates ADD CONSTRAINT fk_template_restaurant
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE;
ALTER TABLE templates ADD CONSTRAINT fk_template_created_by
    FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE templates ADD CONSTRAINT fk_template_updated_by
    FOREIGN KEY (updated_by) REFERENCES users(id);
ALTER TABLE templates ADD CONSTRAINT fk_template_deleted_by
    FOREIGN KEY (deleted_by) REFERENCES users(id);

CREATE UNIQUE INDEX uq_template_name_restaurant
    ON templates (restaurant_id, LOWER(name));

/* template_slots */
ALTER TABLE template_slots ADD CONSTRAINT fk_slot_template
    FOREIGN KEY (template_id) REFERENCES templates(id) ON DELETE CASCADE;

/* slot_options */
ALTER TABLE slot_options ADD CONSTRAINT fk_option_slot
    FOREIGN KEY (slot_id) REFERENCES template_slots(id) ON DELETE CASCADE;
ALTER TABLE slot_options ADD CONSTRAINT fk_option_item
    FOREIGN KEY (item_id) REFERENCES items(id);

/* submenu_nodes */
ALTER TABLE submenu_nodes ADD CONSTRAINT fk_node_submenu
    FOREIGN KEY (submenu_id) REFERENCES submenus(id) ON DELETE CASCADE;
ALTER TABLE submenu_nodes ADD CONSTRAINT fk_node_item
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE;
ALTER TABLE submenu_nodes ADD CONSTRAINT fk_node_template
    FOREIGN KEY (template_id) REFERENCES templates(id) ON DELETE CASCADE;

/* Prevent the same product/template from appearing twice in the same submenu */
ALTER TABLE submenu_nodes ADD CONSTRAINT uq_node_item_per_submenu
    UNIQUE (submenu_id, item_id);
ALTER TABLE submenu_nodes ADD CONSTRAINT uq_node_template_per_submenu
    UNIQUE (submenu_id, template_id);

/* Performance indexes */
CREATE INDEX idx_template_slots_template ON template_slots (template_id);
CREATE INDEX idx_slot_options_slot ON slot_options (slot_id);
CREATE INDEX idx_submenu_nodes_submenu ON submenu_nodes (submenu_id);
