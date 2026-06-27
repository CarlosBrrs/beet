# Orders, POS, Kitchen, Payments And Bills

## Objective
Implement the operational order flow for restaurant POS without loading full order or bill datasets in the frontend.

## Core Decisions
- `OrderStatus`: `DRAFT`, `AWAITING_PAYMENT`, `OPEN`, `COMPLETED`, `CANCELED`.
- `KitchenTicketStatus`: `PENDING`, `PREPARING`, `READY`, `CANCELED`.
- `OrderKitchenSummary`: `NOT_SENT`, `PENDING`, `PREPARING`, `PARTIALLY_READY`, `READY`.
- Payment status is calculated from payments.
- `READY` is the last kitchen state in v1. `SERVED` is out of scope.
- Orders can receive new items while `OPEN`.
- Kitchen uses tickets. Each send to kitchen creates a ticket with its own lines and status.
- Cash sessions are required for payments, not for creating orders.
- `restaurants.operation_mode` is the source of truth for `PREPAID` and `POSTPAID`.
- Product and template prices are gross prices with taxes included.
- Stock cannot go negative.

## Inventory
- POS catalog exposes availability, low stock and unavailable reasons.
- Low stock is `ingredient_stocks.current_stock <= ingredient_stocks.min_stock`.
- Confirming an order creates reservations.
- Moving a kitchen ticket from `PENDING` to `PREPARING` converts reservations into immutable consumptions and `SALE` transactions.
- Canceling a `PENDING` ticket releases reservations.
- Canceling a `PREPARING` or `READY` ticket requires a manual inventory decision.

## Payments And Bills
- Payment methods are configurable per restaurant.
- Initial methods are cash, debit card, credit card, Nequi, Daviplata and internal credit.
- Each payment records its own tip.
- Bills are listed inside an order and are always paginated.
- Bill splitting uses strategies: `ITEM_QUANTITY`, `AMOUNT`, `PERCENTAGE`, `EVENLY`.

## Filtering
Orders are always queried with backend pagination and sorting. Supported filters:
- order, payment and kitchen statuses.
- service type, table, date range, cash session, cash register.
- creator, customer, payment method, total range, delivery status and search.

## Public API Groups
- POS catalog.
- Orders and order items.
- Kitchen tickets.
- Payment methods and payments.
- Bills.
- Manual catalog availability overrides.

## Out Of Scope For V1
- External payment gateways beyond the manual provider contract.
- Served tracking.
- Real delivery carrier integrations.
- Stock-negative overrides.
