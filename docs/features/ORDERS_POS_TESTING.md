# Orders And POS Manual Testing Guide

Use this document to validate the frontend behavior and database state for every order/POS path.

## Common Queries
```sql
SELECT id, order_status, payment_status, kitchen_status, service_type, total_gross_snapshot
FROM orders
WHERE restaurant_id = '<restaurant-id>'
ORDER BY created_at DESC;

SELECT id, order_id, line_type, item_id, template_id, quantity, subtotal_gross_snapshot
FROM order_items
WHERE order_id = '<order-id>';

SELECT id, order_id, status
FROM kitchen_tickets
WHERE order_id = '<order-id>';

SELECT id, order_id, amount, tip_amount, status
FROM payments
WHERE order_id = '<order-id>';

SELECT id, order_id, order_item_id, status, quantity_base
FROM inventory_reservations
WHERE order_id = '<order-id>';

SELECT id, order_id, order_item_id, quantity_base, inventory_transaction_id
FROM order_item_consumptions
WHERE order_id = '<order-id>';

SELECT id, order_id, order_item_id, quantity, canceled_quantity,
       subtotal_gross_snapshot
FROM order_items
WHERE order_id = '<order-id>';

SELECT id, order_item_id, quantity, gross_amount, inventory_disposition,
       kitchen_status_snapshot, reason
FROM order_item_cancellations
WHERE order_id = '<order-id>'
ORDER BY created_at;

SELECT ci.cancellation_id, ci.master_ingredient_id, ci.quantity_base,
       ci.inventory_disposition, ci.inventory_transaction_id
FROM order_item_cancellation_inventory ci
JOIN order_item_cancellations c ON c.id = ci.cancellation_id
WHERE c.order_id = '<order-id>';

SELECT id, order_id, payment_id, amount, status, reason
FROM payment_refunds
WHERE order_id = '<order-id>'
ORDER BY created_at;
```

## Product Available
- Frontend: open POS, search a product with sufficient stock, add it to cart and confirm.
- Expected endpoint: `POST /restaurants/{restaurantId}/orders/draft`, then `POST /orders/{orderId}/confirm`.
- DB: `orders` has `OPEN` for postpaid or `AWAITING_PAYMENT` for prepaid. `inventory_reservations` has one row per consumed ingredient.

## Product Out Of Stock
- Frontend: product is visible but disabled.
- Expected endpoint: `GET /restaurants/{restaurantId}/pos/catalog`.
- DB: ingredient stock or available quantity is below recipe requirement.
- Error path: forcing confirm returns a stock validation error and creates no reservations.

## Product Low Stock
- Frontend: product is enabled but has low-stock visual indicator.
- DB: at least one required ingredient has `current_stock <= min_stock`.

## Valid Template
- Frontend: configure every required slot and confirm.
- DB: `order_items.line_type = 'TEMPLATE'`, slot snapshots in `order_item_template_slots`, option snapshots in `order_item_template_options`.

## Template With Out-Of-Stock Option
- Frontend: template remains visible; unavailable option is disabled.
- Error path: forcing unavailable option returns validation error.

## Prepaid Success
- Confirm creates `AWAITING_PAYMENT`.
- Register full payment.
- DB: order becomes `OPEN`, payment status `PAID`, kitchen ticket `PENDING`.

## Postpaid Success
- Confirm creates `OPEN` immediately.
- DB: kitchen ticket `PENDING`, payment status `UNPAID`.

## Payment With Tip
- Frontend: register payment with tip.
- DB: `payments.tip_amount` stores the tip, `orders.tip_total_snapshot` equals sum of non-voided tips.

## Payment Without Open Cash Session
- Frontend: payment action is blocked or returns API error.
- DB: no `payments` row is inserted.

## Disabled Payment Method
- Frontend: disabled method is not selectable.
- Error path: forcing it returns validation error.

## Split Bills
- Frontend: split by items, amount, percentage and evenly.
- Endpoint: `POST /restaurants/{restaurantId}/orders/{orderId}/bills/split`.
- DB: `order_bills` and `order_bill_allocations` reflect the backend-calculated result.

## Kitchen Ticket PENDING To PREPARING
- Frontend: KDS marks a ticket as preparing.
- DB: reservations become consumed, stock decreases, `inventory_transactions.reason = 'SALE'`.

## Kitchen Ticket PREPARING To READY
- Frontend: KDS marks a ticket ready.
- DB: ticket status changes only; inventory does not change again.

## Complete Order
- Frontend: open `Orders > Ver`, try `Completar`.
- Success requires `order_status = 'OPEN'`, `payment_status = 'PAID'`, `kitchen_status = 'READY'`, `refund_due_snapshot = 0` and at least one active unit.
- DB after success: `orders.order_status = 'COMPLETED'` and `completed_at IS NOT NULL`.
- Error path: if kitchen is `PREPARING` or payment is not `PAID`, the API rejects completion.

## Partial Item Cancellation Before Preparing
- Frontend: open order detail, click `Cancelar unidades` on a line whose ticket is still `PENDING`.
- Request: `POST /restaurants/{restaurantId}/orders/{orderId}/items/{orderItemId}/cancel`.
- DB:
  - `order_items.quantity` remains original.
  - `order_items.canceled_quantity` increases by the canceled units.
  - `order_item_cancellations.inventory_disposition = 'RELEASE_RESERVED'`.
  - `inventory_reservations.quantity_base` is reduced proportionally or status changes to `RELEASED`.
  - `kitchen_ticket_lines.canceled_quantity` increases.
- Visible result: order detail shows original, canceled and active quantities.

## Partial Item Cancellation After Preparing With Restock
- Frontend: move ticket to `PREPARING`, then cancel units and choose `Reponer al inventario`.
- DB:
  - Existing `order_item_consumptions` rows are not modified.
  - `order_item_cancellation_inventory.inventory_disposition = 'RESTOCK'`.
  - `inventory_transactions.reason = 'SALE_REVERSAL'` with positive `delta`.
  - `ingredient_stocks.current_stock` increases by the proportional recipe requirement.

## Partial Item Cancellation After Preparing Without Restock
- Frontend: choose `No reponer`.
- DB:
  - `order_item_cancellation_inventory.inventory_disposition = 'NO_RESTOCK'`.
  - No positive `inventory_transactions` row is created.
  - Stock remains as consumed by the original `SALE`.

## Partial Item Cancellation After Preparing As Waste
- Frontend: choose `Registrar como desperdicio`.
- DB:
  - `order_item_cancellation_inventory.inventory_disposition = 'WASTE'`.
  - No second stock deduction occurs.
  - The waste decision is audit-only in this phase.

## Full Order Cancellation
- Frontend: open order detail and click `Cancelar orden`.
- If any active item is `PREPARING` or `READY`, choose inventory decision per line.
- DB:
  - Every active line receives an `order_item_cancellations` row.
  - `order_items.canceled_quantity = quantity` for fully canceled lines.
  - Empty kitchen tickets become `CANCELED`.
  - `orders.order_status = 'CANCELED'`, `canceled_at IS NOT NULL`, `cancel_reason` is populated.

## Cancellation With Paid Balance
- Frontend: pay an order, then cancel units or the full order.
- DB:
  - `orders.total_gross_snapshot` drops to the active total.
  - `orders.refund_due_snapshot = recorded_payments - recorded_refunds - total_gross_snapshot` when positive.
  - `orders.payment_status = 'REFUND_PENDING'`.
- Visible result: order detail shows a refund warning and `Registrar devolucion`.

## Refund Registration
- Frontend: keep a cash session open, open order detail, click `Registrar devolucion`.
- Request: `POST /restaurants/{restaurantId}/orders/{orderId}/refunds`.
- DB:
  - `payment_refunds` has one `RECORDED` row.
  - `orders.refunded_total_snapshot` increases.
  - `orders.refund_due_snapshot` decreases.
  - Payment status returns to `PAID` when refund due reaches zero, or `REFUNDED` for a fully canceled and fully refunded order.
- Error path: refund without cash session fails and inserts no row.

## Refund Over Original Payment
- Frontend/API: attempt to refund more than the selected payment's refundable amount.
- Expected: backend rejects the request.
- DB: no new `payment_refunds` row.

## Dine-In
- Frontend: table is required.
- DB: only one `OPEN` order can exist for the same `restaurant_id + table_id`.

## Takeout
- Frontend: no table or delivery address required.

## Delivery
- Frontend: address and phone required.
- DB: delivery fields are populated.

## Order Filters And Sorting
- Frontend: use status, payment, kitchen, service, table, date, cash, user, customer, method and total filters.
- Endpoint: `GET /restaurants/{restaurantId}/orders?...`.
- DB: result set is paginated and matches filters.

## Bill Filters And Sorting
- Frontend: open an order, filter bills by payment status/search.
- Endpoint: `GET /restaurants/{restaurantId}/orders/{orderId}/bills?...`.

## Manual Out-Of-Stock
- Frontend: mark product or template unavailable for today.
- DB: `catalog_availability_overrides` has active row and POS catalog disables the element.

## Sellable Portions And Recursive Recipes

### Recreate Carne Molida Guisada
- Frontend: create a tracked product with physical yield `1500 g`, `10` sellable portions and sale price `$16,000`.
- Recipe:
  - `1500 g` of ingredient `Carne molida`.
  - `200 g` of preparation `Guiso tomate y cebolla`.
- Expected UI:
  - Approximate portion size: `150 g`.
  - Batch theoretical cost: `$37,100`.
  - Cost per portion: `$3,710`.
  - Margin: approximately `76.8%`.
- Expected `items`:
```sql
SELECT name, yield_qty, yield_unit_id, sellable_units_per_batch,
       theoretical_cost
FROM items
WHERE name = 'Carne molida guisada';
```
- `theoretical_cost` stores cost per sellable portion, not batch cost.

### Draft Snapshot
- Frontend: add one portion to a new order and create the draft.
- Endpoint: `POST /restaurants/{restaurantId}/orders/draft`.
- Expected `order_items.theoretical_cost_snapshot`: `3710`.
- Expected frozen requirements:
```sql
SELECT ingredient_name_snapshot, quantity_base_per_sale_unit, unit_cost_snapshot
FROM order_item_ingredient_requirements
WHERE order_item_id = :orderItemId
ORDER BY ingredient_name_snapshot;
```
- Expected rows per sale unit:
  - `Carne molida`: `150.000000 g`.
  - `Tomate`: `10.000000 g`.
  - `Cebolla roja`: `20.000000 g`.
- Editing the catalog recipe after this point must not change these rows.

### Confirm Two Portions
- Frontend: create another draft with quantity `2`, then confirm.
- Expected active reservations:
```sql
SELECT mi.name, r.quantity_base, r.unit_cost_snapshot, r.status
FROM inventory_reservations r
JOIN master_ingredients mi ON mi.id = r.master_ingredient_id
WHERE r.order_item_id = :orderItemId
ORDER BY mi.name;
```
- Expected quantities:
  - `Carne molida`: `300.000000`.
  - `Tomate`: `20.000000`.
  - `Cebolla roja`: `40.000000`.
- Decimal product quantities such as `1.5` must be rejected.

### Start Preparing
- Frontend: move the related kitchen ticket from `PENDING` to `PREPARING`.
- Expected:
  - `ingredient_stocks.current_stock` decreases by the exact reserved quantity.
  - reservations become `CONSUMED`.
  - one immutable `order_item_consumptions` row exists per ingredient.
  - `inventory_transactions.reason = 'SALE'`.
  - `unit_cost_snapshot` remains nullable; it must never be replaced with zero.

### Cancel Before Preparing
- Cancel the pending ticket/order before `PREPARING`.
- Expected:
  - reservations become `RELEASED`.
  - stock does not change.
  - no consumption or `SALE` transaction is created.

### Template Option
- Add `Carne molida guisada` as a template option and order two templates with one selected unit each.
- The frozen requirements and reservations must equal the two-portion values above.
- If the same ingredient is reached through multiple selected options, it must appear once in
  `order_item_ingredient_requirements` with the quantities added.

### Missing Cost
- Remove or deactivate the active supplier item for one recipe ingredient without changing stock.
- Expected catalog UI:
  - product remains sellable when stock is sufficient;
  - badge/message `Costo incompleto`;
  - no margin is shown;
  - missing ingredient names are listed.
- Expected DB:
  - `items.theoretical_cost IS NULL` after recalculation;
  - new order snapshot and matching requirement `unit_cost_snapshot` are `NULL`.

### POS Availability
- `GET /restaurants/{restaurantId}/pos/catalog` returns:
  - `maxAvailableUnits`;
  - `lowStock`;
  - `insufficientIngredients`;
  - `available = false` when stock minus active reservations cannot cover one portion.
- A template is unavailable when an obligatory slot no longer has enough available option units
  to satisfy `minSelection`.
