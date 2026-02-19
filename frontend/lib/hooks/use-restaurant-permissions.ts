import { useQuery } from "@tanstack/react-query"
import { ApiGenericResponse, UserRestaurantPermissionsResponse } from "@/lib/api-types"
import { apiClient } from "@/lib/api-client"

const PERMISSIONS_KEY = (restaurantId: string) => ["permissions", restaurantId]

async function fetchPermissions(restaurantId: string): Promise<UserRestaurantPermissionsResponse> {
    const data = await apiClient<ApiGenericResponse<UserRestaurantPermissionsResponse>>(
        `/restaurants/${restaurantId}/my-permissions`
    )

    if (!data.success || !data.data) {
        throw new Error(data.errorMessage || "Failed to fetch permissions")
    }

    return data.data
}


import { PermissionModule, PermissionAction } from "@/lib/permissions"

export function useRestaurantPermissions(restaurantId: string | null) {
    const query = useQuery({
        queryKey: PERMISSIONS_KEY(restaurantId!),
        queryFn: () => fetchPermissions(restaurantId!),
        enabled: !!restaurantId,
        retry: 1,
    })

    // Overload: Check specific action, or just check if module has ANY permissions
    // Usage 1: can("CREATE", "INGREDIENTS") -> Boolean (Has specific action?)
    // Usage 2: can("INGREDIENTS") -> Boolean (Has any access to module?)
    const can = (actionOrModule: string, moduleName?: string) => {
        if (!query.data?.permissions) return false

        // Case 2: Only module name provided (Check existence)
        if (!moduleName) {
            const mod = actionOrModule as PermissionModule
            const acts = query.data.permissions[mod]
            return acts && Array.isArray(acts) && acts.length > 0
        }

        // Case 1: Action and Module provided
        const mod = moduleName as PermissionModule
        const acts = query.data.permissions[mod]

        if (!acts || !Array.isArray(acts)) return false

        return acts.includes(actionOrModule as PermissionAction)
    }

    return { ...query, can }
}
