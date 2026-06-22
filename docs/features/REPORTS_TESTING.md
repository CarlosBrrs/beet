# Operational Reports - Manual Testing

## Preparation

Apply migrations through V23 and use an owner with at least one restaurant,
orders, payments, a closed cash session, and inventory movements.

The restaurant view is available at:

```text
/restaurants/{restaurantId}/reports
```

The owner consolidation is available at:

```text
/account/reports
```

## Open business day

Open a business day, create an order and register a payment. The report must
show `Provisional`, and values must change without closing the day.

```sql
SELECT id, business_date, status
FROM restaurant_business_days
WHERE restaurant_id = '<restaurant-id>'
ORDER BY business_date DESC;

SELECT SUM(amount), SUM(tip_amount)
FROM payments p
JOIN cash_sessions cs ON cs.id = p.cash_session_id
WHERE cs.business_day_id = '<business-day-id>'
  AND p.status = 'RECORDED';
```

## Closed and reopened day

Close all sessions and the business day. The report must use the highest
`closure_sequence`. Reopen it and confirm that it becomes provisional again
without losing previous closures.

```sql
SELECT closure_sequence, payments_total, expected_cash_total,
       counted_cash_total, difference_total
FROM business_day_closures
WHERE business_day_id = '<business-day-id>'
ORDER BY closure_sequence;
```

## Sales and payments

Compare gross sales, collected amount, tips and refunds independently.

```sql
SELECT order_status, SUM(total_gross_snapshot)
FROM orders
WHERE restaurant_id = '<restaurant-id>'
  AND business_date BETWEEN '<from>' AND '<to>'
GROUP BY order_status;

SELECT pm.code, SUM(p.amount), SUM(p.tip_amount)
FROM payments p
JOIN payment_methods pm ON pm.id = p.payment_method_id
JOIN orders o ON o.id = p.order_id
WHERE p.restaurant_id = '<restaurant-id>'
  AND o.business_date BETWEEN '<from>' AND '<to>'
  AND p.status = 'RECORDED'
GROUP BY pm.code;
```

## Products and templates

Sell products and templates, then cancel part of a line. The sold quantity must
be `quantity - canceled_quantity`. Template rows must expose selected option
usage.

```sql
SELECT item_name_snapshot, line_type, quantity, canceled_quantity,
       subtotal_gross_snapshot, theoretical_cost_snapshot
FROM order_items
WHERE order_id = '<order-id>';
```

Rows with incomplete cost must show `Costo incompleto` and no invented margin.

## Inventory

Start kitchen preparation to create immutable consumptions. Verify consumed
quantity and historical cost.

```sql
SELECT mi.name, SUM(c.quantity_base),
       SUM(c.quantity_base * c.unit_cost_snapshot)
FROM order_item_consumptions c
JOIN master_ingredients mi ON mi.id = c.master_ingredient_id
JOIN orders o ON o.id = c.order_id
WHERE c.restaurant_id = '<restaurant-id>'
  AND o.business_date BETWEEN '<from>' AND '<to>'
GROUP BY mi.name;
```

Inventory valuation must exclude ingredients without an active supplier cost
and list them separately.

```sql
SELECT mi.name, s.current_stock, si.last_cost_base,
       s.current_stock * si.last_cost_base AS known_value
FROM ingredient_stocks s
JOIN master_ingredients mi ON mi.id = s.master_ingredient_id
LEFT JOIN supplier_items si
  ON si.id = mi.active_supplier_item_id AND si.deleted_at IS NULL
WHERE s.restaurant_id = '<restaurant-id>'
  AND s.deleted_at IS NULL;
```

## Account consolidation and permissions

- Select all restaurants and compare the total with the sum of each restaurant.
- Select a subset and confirm that only those restaurants are returned.
- Request an inaccessible restaurant ID and expect rejection.
- A user with `FINANCE:VIEW` can see sales.
- Cash reports additionally require `CASH:VIEW`.
- Inventory reports require `INVENTORY:VIEW`.

## Pagination and filters

- Test 10, 20 and 50 rows.
- Search by restaurant, product, method or ingredient name.
- Test day, week and month grouping.
- Verify a date range over 366 days is rejected.
- Verify unsupported sort fields are rejected.
