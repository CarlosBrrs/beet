ALTER TYPE item_class RENAME VALUE 'SALEABLE_PRODUCT' TO 'PRODUCT';

ALTER TABLE items
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN is_available_as_template_option BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE templates
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT chk_template_base_price CHECK (base_price >= 0);

ALTER TABLE slot_options
    ADD COLUMN max_quantity INTEGER NOT NULL DEFAULT 1,
    ADD CONSTRAINT chk_slot_option_surcharge CHECK (surcharge >= 0),
    ADD CONSTRAINT chk_slot_option_max_quantity CHECK (max_quantity >= 1),
    ADD CONSTRAINT uq_slot_option_item UNIQUE (slot_id, item_id);

ALTER TABLE template_slots
    ADD CONSTRAINT chk_slot_max_selection CHECK (max_selection >= 1),
    ADD CONSTRAINT chk_slot_sort_order CHECK (sort_order >= 0);

ALTER TABLE slot_options
    ADD CONSTRAINT chk_slot_option_sort_order CHECK (sort_order >= 0);

ALTER TABLE submenu_nodes
    ADD CONSTRAINT chk_submenu_node_sort_order CHECK (sort_order >= 0);

ALTER TABLE submenus ADD COLUMN restaurant_id UUID;
UPDATE submenus s SET restaurant_id = m.restaurant_id FROM menus m WHERE m.id = s.menu_id;
ALTER TABLE submenus ALTER COLUMN restaurant_id SET NOT NULL;

ALTER TABLE template_slots ADD COLUMN restaurant_id UUID;
UPDATE template_slots s SET restaurant_id = t.restaurant_id FROM templates t WHERE t.id = s.template_id;
ALTER TABLE template_slots ALTER COLUMN restaurant_id SET NOT NULL;

ALTER TABLE slot_options ADD COLUMN restaurant_id UUID;
UPDATE slot_options o SET restaurant_id = s.restaurant_id FROM template_slots s WHERE s.id = o.slot_id;
ALTER TABLE slot_options ALTER COLUMN restaurant_id SET NOT NULL;

ALTER TABLE submenu_nodes ADD COLUMN restaurant_id UUID;
UPDATE submenu_nodes n SET restaurant_id = s.restaurant_id FROM submenus s WHERE s.id = n.submenu_id;
ALTER TABLE submenu_nodes ALTER COLUMN restaurant_id SET NOT NULL;

ALTER TABLE items ADD CONSTRAINT uq_items_id_restaurant UNIQUE (id, restaurant_id);
ALTER TABLE templates ADD CONSTRAINT uq_templates_id_restaurant UNIQUE (id, restaurant_id);
ALTER TABLE menus ADD CONSTRAINT uq_menus_id_restaurant UNIQUE (id, restaurant_id);
ALTER TABLE submenus ADD CONSTRAINT uq_submenus_id_restaurant UNIQUE (id, restaurant_id);
ALTER TABLE template_slots ADD CONSTRAINT uq_template_slots_id_restaurant UNIQUE (id, restaurant_id);

ALTER TABLE template_slots ADD CONSTRAINT fk_template_slots_tenant_template
    FOREIGN KEY (template_id, restaurant_id) REFERENCES templates(id, restaurant_id) ON DELETE CASCADE;
ALTER TABLE slot_options ADD CONSTRAINT fk_slot_options_tenant_slot
    FOREIGN KEY (slot_id, restaurant_id) REFERENCES template_slots(id, restaurant_id) ON DELETE CASCADE;
ALTER TABLE slot_options ADD CONSTRAINT fk_slot_options_tenant_item
    FOREIGN KEY (item_id, restaurant_id) REFERENCES items(id, restaurant_id);
ALTER TABLE submenu_nodes ADD CONSTRAINT fk_submenu_nodes_tenant_submenu
    FOREIGN KEY (submenu_id, restaurant_id) REFERENCES submenus(id, restaurant_id) ON DELETE CASCADE;
ALTER TABLE submenu_nodes ADD CONSTRAINT fk_submenu_nodes_tenant_item
    FOREIGN KEY (item_id, restaurant_id) REFERENCES items(id, restaurant_id) ON DELETE CASCADE;
ALTER TABLE submenu_nodes ADD CONSTRAINT fk_submenu_nodes_tenant_template
    FOREIGN KEY (template_id, restaurant_id) REFERENCES templates(id, restaurant_id) ON DELETE CASCADE;
ALTER TABLE submenus ADD CONSTRAINT fk_submenus_tenant_menu
    FOREIGN KEY (menu_id, restaurant_id) REFERENCES menus(id, restaurant_id) ON DELETE CASCADE;

CREATE INDEX idx_items_template_options
    ON items (restaurant_id, is_active, is_available_as_template_option)
    WHERE deleted_at IS NULL AND class = 'PRODUCT';
CREATE INDEX idx_templates_restaurant_active
    ON templates (restaurant_id, is_active)
    WHERE deleted_at IS NULL;
