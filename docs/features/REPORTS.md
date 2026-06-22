# Operational Reports

## Purpose

The reports module is read-only and is separated from the operational use cases
that create orders, collect payments, move stock, and close cash sessions.

Reports are grouped into independent capabilities:

- sales
- cash and payments
- products and templates
- ingredients and inventory

Each capability contributes one or more `ReportProvider` implementations. The
registry discovers providers through Spring injection, so a new report does not
require a central switch statement.

## Contexts

Restaurant reports are available under `/restaurants/{restaurantId}/reports`.
Account reports are available under `/account/reports` and only include
restaurants accessible to the authenticated user.

Open business days are calculated from live operational tables and are marked
as provisional. Closed business days use the latest V22 closure snapshot.

## Accounting definitions

- Gross sales exclude canceled orders.
- Completed sales include only completed orders.
- Sold quantity is `quantity - canceled_quantity`.
- Collected amount excludes tips.
- Net collected is recorded payments minus recorded refunds.
- Historical costs use order and consumption snapshots.
- Missing costs remain missing and never become zero.
- Inventory valuation is current stock multiplied by the active ingredient
  cost. Ingredients without an active cost are reported separately.

## Permissions

- Sales, payment, product and template reports require `FINANCE:VIEW`.
- Cash reconciliation reports additionally require `CASH:VIEW`.
- Inventory reports require `INVENTORY:VIEW`.
- `ALL:ALL` keeps the owner bypass.

## Deferred

- PDF and spreadsheet exports
- scheduled reports
- materialized views and aggregate tables
- multi-currency presentation
