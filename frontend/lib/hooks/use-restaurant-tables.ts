import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { apiClient } from "@/lib/api-client"
import {
    ApiGenericResponse,
    CreateRestaurantTableRequest,
    RestaurantTableResponse,
    UpdateRestaurantTableRequest,
} from "@/lib/api-types"

export const restaurantTableKeys = {
    all: ["restaurant-tables"] as const,
    list: (restaurantId: string | null) => [...restaurantTableKeys.all, "list", restaurantId] as const,
}

async function fetchRestaurantTables(restaurantId: string): Promise<RestaurantTableResponse[]> {
    const response = await apiClient<ApiGenericResponse<RestaurantTableResponse[]>>(
        `/restaurants/${restaurantId}/tables`
    )
    return response.data
}

async function createRestaurantTable(
    restaurantId: string,
    request: CreateRestaurantTableRequest
): Promise<RestaurantTableResponse> {
    const response = await apiClient<ApiGenericResponse<RestaurantTableResponse>>(
        `/restaurants/${restaurantId}/tables`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function updateRestaurantTable(
    restaurantId: string,
    tableId: string,
    request: UpdateRestaurantTableRequest
): Promise<RestaurantTableResponse> {
    const response = await apiClient<ApiGenericResponse<RestaurantTableResponse>>(
        `/restaurants/${restaurantId}/tables/${tableId}`,
        { method: "PATCH", body: JSON.stringify(request) }
    )
    return response.data
}

async function deactivateRestaurantTable(
    restaurantId: string,
    tableId: string
): Promise<RestaurantTableResponse> {
    const response = await apiClient<ApiGenericResponse<RestaurantTableResponse>>(
        `/restaurants/${restaurantId}/tables/${tableId}`,
        { method: "DELETE" }
    )
    return response.data
}

export function useRestaurantTables() {
    const { restaurantId } = useRestaurantContext()

    return useQuery({
        queryKey: restaurantTableKeys.list(restaurantId),
        queryFn: () => fetchRestaurantTables(restaurantId!),
        enabled: !!restaurantId,
    })
}

export function useCreateRestaurantTable() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (request: CreateRestaurantTableRequest) => createRestaurantTable(restaurantId!, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: restaurantTableKeys.list(restaurantId) })
            toast.success("Table created")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to create table"),
    })
}

export function useUpdateRestaurantTable() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ id, request }: { id: string; request: UpdateRestaurantTableRequest }) =>
            updateRestaurantTable(restaurantId!, id, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: restaurantTableKeys.list(restaurantId) })
            toast.success("Table updated")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to update table"),
    })
}

export function useDeactivateRestaurantTable() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (tableId: string) => deactivateRestaurantTable(restaurantId!, tableId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: restaurantTableKeys.list(restaurantId) })
            toast.success("Table deactivated")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to deactivate table"),
    })
}
