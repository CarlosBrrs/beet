"use client"

import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import { ApiGenericResponse, RestaurantResponse } from "@/lib/api-types"

const MY_RESTAURANTS_KEY = ["my-restaurants"]

/**
 * Fetches the list of restaurants the current user has access to.
 * Works for both owners (returns all their restaurants) and employees (returns assigned restaurants).
 * React Query deduplicates the call if multiple components use this hook simultaneously.
 */
export function useMyRestaurants() {
    return useQuery({
        queryKey: MY_RESTAURANTS_KEY,
        queryFn: async () => {
            const data = await apiClient<ApiGenericResponse<RestaurantResponse[]>>("/restaurants/my-restaurants")
            if (!data.success || !data.data) {
                throw new Error(data.errorMessage || "Failed to fetch restaurants")
            }
            return data.data
        },
    })
}
