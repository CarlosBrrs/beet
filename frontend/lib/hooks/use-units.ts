import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import { ApiGenericResponse, UnitResponse } from "@/lib/api-types"

// Query Keys
export const UnitsKeys = {
    all: ["units"] as const,
}

// Fetcher
async function fetchUnits(): Promise<UnitResponse[]> {
    const data = await apiClient<ApiGenericResponse<UnitResponse[]>>("/units")
    if (!data.success) throw new Error(data.errorMessage || "Failed to fetch units")
    return data.data
}

/**
 * Hook to fetch all units from the backend.
 * Units are global (not restaurant-scoped) and rarely change,
 * so we set a long staleTime to avoid unnecessary refetches.
 */
export function useUnits() {
    return useQuery({
        queryKey: UnitsKeys.all,
        queryFn: fetchUnits,
        staleTime: 1000 * 60 * 30, // 30 min — units rarely change
    })
}
