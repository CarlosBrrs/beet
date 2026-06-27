# Product Catalog And Configurable Templates

## Objective
Provide a reusable commercial catalog per restaurant before implementing POS orders.

## Entity Rules
- `PREPARATION` is an internal reusable recipe. It is never published and cannot be a template option.
- `PRODUCT` is a commercial item. It may be sold individually, used as a template option, or both.
- `TEMPLATE` is a configurable shell with slots. Templates cannot be nested.
- Publication is derived only from `submenu_nodes`. An unpublished product or template remains available administratively.
- `is_active` controls use in new operations.
- `is_available_as_template_option` controls whether a product can be selected while designing templates.

## Slot Rules
- Every saved template has at least one slot and every slot has at least one option.
- A slot requires `min_selection >= 0`, `max_selection >= 1`, and `min_selection <= max_selection`.
- Options reference active, eligible `PRODUCT` records from the same restaurant.
- A product cannot appear twice in the same slot.
- `max_quantity` is at least one and cannot exceed the slot maximum.
- Surcharges are non-negative.
- Defaults represent one preselected unit each and cannot exceed the slot maximum.
- A slot with one option and `min_selection = max_selection = 1` is normalized as a visible, selected and locked fixed option for the future POS.

## Publication
- Products and templates may have at most one submenu publication in the first version.
- The database relation remains many-to-many ready so this restriction can be relaxed later.
- Publishing an individual product requires an active product and a sale price greater than zero.
- Unpublishing removes only the `submenu_node`; it does not delete catalog data.
- Deactivating a product is blocked while published or used by a slot.
- Disabling template eligibility is blocked while the product is used by a slot.
- Deactivating a template is blocked while published.

## API
### Products
- `GET /restaurants/{restaurantId}/products?page=0&size=10&search=...`
- `POST /restaurants/{restaurantId}/products`
- `GET|PUT /restaurants/{restaurantId}/products/{productId}`
- `PATCH /restaurants/{restaurantId}/products/{productId}/activation`
- `GET /restaurants/{restaurantId}/products/{productId}/dependencies`
- `GET /restaurants/{restaurantId}/products/template-options`

### Templates
- `GET /restaurants/{restaurantId}/templates?page=0&size=10&search=...`
- `POST /restaurants/{restaurantId}/templates`
- `GET|PUT /restaurants/{restaurantId}/templates/{templateId}`
- `PATCH /restaurants/{restaurantId}/templates/{templateId}/activation`

### Submenu Publication
- `GET|POST /restaurants/{restaurantId}/menus/{menuId}/submenus/{submenuId}/nodes`
- `DELETE /restaurants/{restaurantId}/menus/{menuId}/submenus/{submenuId}/nodes/{nodeId}`
- Nested product and template creation endpoints remain as atomic create-and-publish conveniences.

## Administrative UX
- Product and template hubs use backend pagination with visible totals and page controls.
- When product deactivation is blocked, the UI lists submenu publications and template slots returned by the dependencies endpoint.
- Product inventory tracking mode is chosen during creation. Converting an existing flat product into a tracked product requires a dedicated migration workflow because its cost source, yield and recipe must change together.

## Security
- `PRODUCTS` controls product catalog operations.
- `PREPARATIONS` controls internal recipe operations.
- `TEMPLATES` controls template catalog operations.
- `MENUS` controls publication and unpublication.
- Every nested URL validates the complete `restaurant -> menu -> submenu -> node` chain.
- Composite foreign keys prevent cross-restaurant links in menus, slots, options and submenu nodes.

## Deferred To Orders
POS order implementation will store immutable snapshots for the sold template, slots, selected products, quantities, surcharges and costs. Historical orders must not depend on later catalog edits.
