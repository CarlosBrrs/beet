---
name: scaffold_guard
description: Generates the <Can> component for Role-Based Access Control (RBAC).
---

# Scaffold Guard Component

This skill generates the `<Can>` component logic.

## ⚠️ CRITICAL: Always Use `<Can>` for Permission Checks

**NEVER** use inline permission checks like `{role === "OWNER" && ...}` or `{restaurants.some(r => r.role === "OWNER") && ...}`.

**ALWAYS** wrap conditionally-rendered UI with the `<Can>` component:

```tsx
// ✅ Correct — uses <Can> for RBAC
<Can I="CREATE" a="RESTAURANTS">
    <Button>Create Restaurant</Button>
</Can>

// ❌ Wrong — inline role checks bypass the RBAC system
{user.role === "OWNER" && <Button>Create Restaurant</Button>}
```

The `<Can>` component handles:
- Restaurant-scoped permission checks (auto-reads from `RestaurantProvider` context)
- Account-level / owner-only checks (pass `restaurantId={null}` or omit)
- OWNER wildcard bypass (`{"ALL": ["ALL"]}`)
- Loading states (renders nothing while permissions load)

## Usage

This is typically a one-time generation, but can be updated.

## Existing Files

### `components/shared/can.tsx`
The actual implementation supports:
- `I` — action to check (e.g. `"CREATE"`, `"VIEW"`, `"EDIT"`)
- `a` — module to check (e.g. `"RESTAURANTS"`, `"INVENTORY"`)
- `restaurantId` — optional override:
  - Omitted → reads from `RestaurantProvider` context (or null if outside)
  - Explicit `null` → account-level check (matches global OWNER entry)
  - Explicit UUID → checks that specific restaurant's role
- `fallback` — optional JSX to render when permission denied
- Uses `useMyPermissions()` hook which fetches from `/auth/my-permissions`

### `lib/hooks/use-my-permissions.ts`
- Fetches all user permission entries (one per restaurant + one global for owners)
- `can(action, module, restaurantId?)` — programmatic check
- `isOwner()` — returns true if user has a global null-restaurantId entry
- Cached with `staleTime: Infinity` (permissions don't change during session)

### `lib/permissions.ts`
- `PermissionModule` enum (INVENTORY, RESTAURANTS, CATALOG, etc.)
- `PermissionAction` enum (VIEW, CREATE, EDIT, DELETE, ACTIVATE, etc.)
- `PermissionMap` type

## Common Patterns

### In-restaurant UI gating (auto context)
```tsx
<Can I="EDIT" a="INVENTORY">
    <Button>Adjust Stock</Button>
</Can>
```

### Owner-only features (account level)
```tsx
<Can I="CREATE" a="RESTAURANTS">
    <Button>New Restaurant</Button>
</Can>
```

### With fallback
```tsx
<Can I="DELETE" a="CATALOG" fallback={<p>No permission</p>}>
    <Button variant="destructive">Delete</Button>
</Can>
```
