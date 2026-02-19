---
name: scaffold_guard
description: Generates the <Can> component for Role-Based Access Control (RBAC).
---

# Scaffold Guard Component

This skill generates the `<Can>` component logic.

## Usage

This is typically a one-time generation, but can be updated.

## Actions

1.  **Create Permission Types**:
    *   Path: `lib/permissions.ts`

2.  **Create Guard Component**:
    *   Path: `components/shared/can.tsx`

## Template

### `lib/permissions.ts`
```typescript
// Define Modules
export enum PermissionModule {
    ORDERS = "ORDERS",
    MENU = "MENU",
    // Add others
}

// Define Actions
export enum PermissionAction {
    CREATE = "CREATE",
    READ = "READ",
    UPDATE = "UPDATE",
    DELETE = "DELETE",
    // Add others like VOID, REFUND
}

export type PermissionMap = Partial<Record<PermissionModule, PermissionAction[]>>
```

### `components/shared/can.tsx`
```tsx
"use client"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { PermissionModule, PermissionAction } from "@/lib/permissions"
import { ReactNode } from "react"

interface CanProps {
    I: PermissionAction
    a: PermissionModule
    children: ReactNode
    fallback?: ReactNode
}

export function Can({ I, a, children, fallback = null }: CanProps) {
    const { permissions, isLoading } = useRestaurantContext()

    if (isLoading) return null

    // permissions is a Partial<Record<PermissionModule, PermissionAction[]>>
    const modulePermissions = permissions?.[a]

    // Check if the user has the specific action permission for this module
    const hasPermission = modulePermissions?.includes(I)

    if (hasPermission) {
        return <>{children}</>
    }

    return <>{fallback}</>
}
```
