# Ingredients And Suppliers

## Current Scope

Ingredients and suppliers are account-level catalogs scoped by effective owner. They are shared across all restaurants that belong to that owner.

## Implemented

- Suppliers support paginated listing, detail, create, update, activation and soft delete.
- Ingredients support paginated listing, detail, create, basic update and soft delete.
- Ingredient detail exposes aggregated stock, active supplier item and whether cost is complete.
- Existing employees operate through `SecurityUtils.getEffectiveOwnerId()` so catalog reads/writes resolve to the owner catalog.
- Existing JWTs from suspended users are no longer accepted by the authentication filter.

## Operational Rules

- Supplier document type plus document number is unique per owner among non-deleted suppliers.
- Suppliers with purchases or ingredient presentations cannot be deactivated or deleted.
- Ingredient base unit changes are rejected after supplier items, stock, recipes or order history exist.
- Ingredients with stock, recipes, orders or inventory history cannot be deleted.
- Deletion is soft delete; historical references are preserved.

## Deferred

- Global account-level permission enforcement needs an account-scope permission interceptor or restaurant-scoped catalog routes. The existing `@RequiresPermission` interceptor requires a `restaurantId` path variable.
- Advanced supplier item management, including multiple presentations per ingredient and active supplier switching, remains deferred.
