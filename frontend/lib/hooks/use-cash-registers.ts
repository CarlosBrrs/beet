import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { apiClient } from "@/lib/api-client"
import {
    ApiGenericResponse,
    CashRegisterResponse,
    CreateCashRegisterRequest,
    UpdateCashRegisterRequest,
} from "@/lib/api-types"

export const cashRegisterKeys = {
    all: ["cash-registers"] as const,
    list: (restaurantId: string | null) => [...cashRegisterKeys.all, "list", restaurantId] as const,
}

async function fetchCashRegisters(restaurantId: string): Promise<CashRegisterResponse[]> {
    const response = await apiClient<ApiGenericResponse<CashRegisterResponse[]>>(
        `/restaurants/${restaurantId}/cash-registers`
    )
    if (!response.success) throw new Error(response.errorMessage || "Failed to load cash registers")
    return response.data
}

async function createCashRegister(
    restaurantId: string,
    request: CreateCashRegisterRequest
): Promise<CashRegisterResponse> {
    const response = await apiClient<ApiGenericResponse<CashRegisterResponse>>(
        `/restaurants/${restaurantId}/cash-registers`,
        { method: "POST", body: JSON.stringify(request) }
    )
    if (!response.success) throw new Error(response.errorMessage || "Failed to create cash register")
    return response.data
}

async function updateCashRegister(
    restaurantId: string,
    registerId: string,
    request: UpdateCashRegisterRequest
): Promise<CashRegisterResponse> {
    const response = await apiClient<ApiGenericResponse<CashRegisterResponse>>(
        `/restaurants/${restaurantId}/cash-registers/${registerId}`,
        { method: "PATCH", body: JSON.stringify(request) }
    )
    if (!response.success) throw new Error(response.errorMessage || "Failed to update cash register")
    return response.data
}

async function deactivateCashRegister(
    restaurantId: string,
    registerId: string
): Promise<CashRegisterResponse> {
    const response = await apiClient<ApiGenericResponse<CashRegisterResponse>>(
        `/restaurants/${restaurantId}/cash-registers/${registerId}`,
        { method: "DELETE" }
    )
    if (!response.success) throw new Error(response.errorMessage || "Failed to deactivate cash register")
    return response.data
}

export function useCashRegisters() {
    const { restaurantId } = useRestaurantContext()

    return useQuery({
        queryKey: cashRegisterKeys.list(restaurantId),
        queryFn: () => fetchCashRegisters(restaurantId!),
        enabled: !!restaurantId,
    })
}

export function useCreateCashRegister() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (request: CreateCashRegisterRequest) => createCashRegister(restaurantId!, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: cashRegisterKeys.list(restaurantId) })
            toast.success("Cash register created")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to create cash register"),
    })
}

export function useUpdateCashRegister() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ id, request }: { id: string; request: UpdateCashRegisterRequest }) =>
            updateCashRegister(restaurantId!, id, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: cashRegisterKeys.list(restaurantId) })
            toast.success("Cash register updated")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to update cash register"),
    })
}

export function useDeactivateCashRegister() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (registerId: string) => deactivateCashRegister(restaurantId!, registerId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: cashRegisterKeys.list(restaurantId) })
            toast.success("Cash register deactivated")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to deactivate cash register"),
    })
}
