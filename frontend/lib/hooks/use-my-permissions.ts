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
 * Permissions can change while another manager edits staff, so they are refreshed
 * periodically and when the window regains focus.
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
        staleTime: 60_000,
        gcTime: 10 * 60_000,
        refetchOnWindowFocus: true,
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

        const hasPermission = (entry?: UserPermissionEntry): boolean => {
            if (!entry) return false

            // OWNER wildcard: {"ALL": ["ALL"]} = full unrestricted access
            // Note: "ALL" is a sentinel string, not in the PermissionModule enum by design.
            const allActions = (entry.permissions as Record<string, string[]>)["ALL"]
            if (allActions && allActions.includes("ALL")) return true

            const actions = entry.permissions[moduleName as PermissionModule]
            if (!actions) return false
            if ((actions as string[]).includes("ALL")) return true
            if ((actions as string[]).includes("MANAGE")) return true
            return actions.includes(action as PermissionAction)
        }

        const globalEntry = query.data.find(p => p.restaurantId === null)
        if (hasPermission(globalEntry)) return true

        if (restaurantId !== undefined && restaurantId !== null) {
            return hasPermission(query.data.find(p => p.restaurantId === restaurantId))
        }

        // Account context: allow modules available in at least one assigned restaurant.
        return query.data
            .filter(p => p.restaurantId !== null)
            .some(hasPermission)
    }

    /**
     * Returns true if the current user is an Owner (has a global null-restaurantId entry).
     */
    const isOwner = (): boolean => {
        return query.data?.some(p => p.restaurantId === null) ?? false
    }

    return { ...query, can, isOwner }
}
