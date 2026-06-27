"use client"

import { useMyPermissions } from "@/lib/hooks/use-my-permissions"
import { PermissionAction, PermissionModule } from "@/lib/permissions"
import React from "react"

// Optional context import — won't throw if used outside a restaurant context
import { useOptionalRestaurantContext } from "@/components/providers/restaurant-provider"

interface CanProps {
    /** The action to check, e.g. "CREATE". Ignored for OWNER (has ALL:ALL). */
    I: PermissionAction | string
    /** The module to check, e.g. "INVENTORY". */
    a: PermissionModule | string
    /**
     * Optional override for the restaurant scope.
     * - If inside a <RestaurantProvider>, the restaurantId is read from context automatically.
     * - Pass explicitly to override (or to use null for account-level checks outside provider).
     */
    restaurantId?: string | null
    children: React.ReactNode
    fallback?: React.ReactNode
}

/**
 * Conditionally renders children if the current user has the required permission.
 *
 * Works in both account context (no restaurant selected) and restaurant context.
 * Owner users with {"ALL": ["ALL"]} permissions always see the children.
 *
 * @example
 * // Inside a restaurant context (restaurantId from provider):
 * <Can I="CREATE" a="INVENTORY">
 *   <button>Add Ingredient</button>
 * </Can>
 *
 * // Account-level owner-only content:
 * <Can I="CREATE" a="INVENTORY" restaurantId={null}>
 *   <Link href="/account/ingredients">Catalog</Link>
 * </Can>
 */
export function Can({ I, a, restaurantId, children, fallback = null }: CanProps) {
    const context = useOptionalRestaurantContext()
    const { can, isLoading } = useMyPermissions()

    // Determine which scope to check:
    // - If restaurantId is explicitly passed, use that (even if null = account scope)
    // - Otherwise fall back to the restaurant context (or null if not in a restaurant)
    const effectiveRestaurantId = restaurantId !== undefined
        ? restaurantId
        : (context?.restaurantId ?? null)

    if (isLoading) return null

    return can(I, a, effectiveRestaurantId) ? <>{children}</> : <>{fallback}</>
}
