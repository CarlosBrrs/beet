"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { apiClient } from "@/lib/api-client"
import { ApiGenericResponse, RestaurantResponse, RestaurantUpdateRequest } from "@/lib/api-types"
import { orderKeys } from "@/lib/hooks/use-orders"
import { cashOperationsKeys } from "@/lib/hooks/use-cash-operations"

export const restaurantKeys = {
    all: ["restaurants"] as const,
    detail: (restaurantId: string | null) => [...restaurantKeys.all, "detail", restaurantId] as const,
}

async function fetchRestaurant(restaurantId: string) {
    const response = await apiClient<ApiGenericResponse<RestaurantResponse>>(`/restaurants/${restaurantId}`)
    return response.data
}

async function updateRestaurant(restaurantId: string, request: RestaurantUpdateRequest) {
    const response = await apiClient<ApiGenericResponse<RestaurantResponse>>(`/restaurants/${restaurantId}`, {
        method: "PUT",
        body: JSON.stringify(request),
    })
    return response.data
}

export function useRestaurant(restaurantId: string | null) {
    return useQuery({
        queryKey: restaurantKeys.detail(restaurantId),
        queryFn: () => fetchRestaurant(restaurantId!),
        enabled: !!restaurantId,
    })
}

export function useUpdateRestaurant(restaurantId: string | null) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (request: RestaurantUpdateRequest) => updateRestaurant(restaurantId!, request),
        onSuccess: async (restaurant) => {
            await Promise.all([
                queryClient.invalidateQueries({ queryKey: ["my-restaurants"] }),
                queryClient.invalidateQueries({ queryKey: restaurantKeys.detail(restaurantId) }),
                queryClient.invalidateQueries({ queryKey: orderKeys.all }),
                queryClient.invalidateQueries({ queryKey: cashOperationsKeys.all }),
            ])
            queryClient.setQueryData(restaurantKeys.detail(restaurant.id), restaurant)
            toast.success("Configuracion del restaurante actualizada")
        },
        onError: (error: Error) => {
            toast.error(error.message || "No se pudo actualizar la configuracion")
        },
    })
}
