"use client"

import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import { UserPermissionEntry, ApiGenericResponse } from "@/lib/api-types"
import { PermissionAction, PermissionModule } from "@/lib/permissions"

const MY_PERMISSIONS_KEY = ["my-permissions"]

async function fetchMyPermissions(): Promise<UserPermissionEntry[]> {
    const data = await apiClient<ApiGenericResponse<UserPermissionEntry[]>>("/auth/my-permissions")
    if (!data.success || !data.data) {
        throw new Error(data.errorMessage || "Failed to fetch permissions")
    }
    return data.data
}

/**
 * Global hook that fetches all permission scopes for the current user.
 *
 * Returns ALL assignments (one for global roles like OWNER, one per restaurant for local roles).
 * The data is cached for the entire session since permissions don't change during a session.
 *
 * Usage:
 *   const { can } = useMyPermissions()
 *   can("CREATE", "INVENTORY")                     // account-level check (owner scope)
 *   can("CREATE", "INVENTORY", restaurantId)        // restaurant-level check
 */
export function useMyPermissions() {
    const query = useQuery({
        queryKey: MY_PERMISSIONS_KEY,
        queryFn: fetchMyPermissions,
        staleTime: Infinity,  // permissions don't change during a session
        gcTime: Infinity,
        retry: 1,
    })

    /**
     * Checks if the current user has a given permission.
     *
     * @param action       - The action to check (e.g. "CREATE"). Ignored for OWNER (ALL:ALL).
     * @param moduleName   - The module to check (e.g. "INVENTORY").
     * @param restaurantId - Optional. If provided, checks against that restaurant's role.
     *                       If omitted/null, checks against the global owner scope.
     */
    const can = (
        action: PermissionAction | string,
        moduleName: PermissionModule | string,
        restaurantId?: string | null
    ): boolean => {
        if (!query.data) return false

        let entry: UserPermissionEntry | undefined

        if (restaurantId !== undefined && restaurantId !== null) {
            // Restaurant context: find the specific restaurant's role entry
            entry = query.data.find(p => p.restaurantId === restaurantId)
            // If no restaurant-specific role, fall back to global OWNER entry (owners access all)
            if (!entry) {
                entry = query.data.find(p => p.restaurantId === null)
            }
        } else {
            // Account context (no restaurant selected): look for global owner entry
            entry = query.data.find(p => p.restaurantId === null)
        }

        if (!entry) return false

        // OWNER wildcard: {"ALL": ["ALL"]} = full unrestricted access
        // Note: "ALL" is a sentinel string, not in the PermissionModule enum by design.
        const allActions = (entry.permissions as Record<string, string[]>)["ALL"]
        if (allActions && allActions.includes("ALL")) return true

        const actions = entry.permissions[moduleName as PermissionModule]
        if (!actions) return false
        if ((actions as string[]).includes("ALL")) return true
        return actions.includes(action as PermissionAction)
    }

    /**
     * Returns true if the current user is an Owner (has a global null-restaurantId entry).
     */
    const isOwner = (): boolean => {
        return query.data?.some(p => p.restaurantId === null) ?? false
    }

    return { ...query, can, isOwner }
}
