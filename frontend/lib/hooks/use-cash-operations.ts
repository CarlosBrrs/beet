import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { useOptionalRestaurantContext, useRestaurantContext } from "@/components/providers/restaurant-provider"
import { apiClient } from "@/lib/api-client"
import {
    ApiGenericResponse,
    BusinessDayClosureResponse,
    BusinessDayResponse,
    CashMovementRequest,
    CashMovementResponse,
    CashSessionReconciliationResponse,
} from "@/lib/api-types"
import { cashSessionKeys } from "@/lib/hooks/use-cash-sessions"

export const cashOperationsKeys = {
    all: ["cash-operations"] as const,
    currentDay: (restaurantId: string | null) => [...cashOperationsKeys.all, "day", restaurantId] as const,
    days: (restaurantId: string | null) => [...cashOperationsKeys.all, "days", restaurantId] as const,
    movements: (restaurantId: string | null, sessionId: string | null) =>
        [...cashOperationsKeys.all, "movements", restaurantId, sessionId] as const,
    reconciliation: (restaurantId: string | null, sessionId: string | null) =>
        [...cashOperationsKeys.all, "reconciliation", restaurantId, sessionId] as const,
}

async function currentBusinessDay(restaurantId: string) {
    const response = await apiClient<ApiGenericResponse<BusinessDayResponse | null>>(
        `/restaurants/${restaurantId}/business-days/current`
    )
    return response.data
}

async function businessDays(restaurantId: string) {
    const response = await apiClient<ApiGenericResponse<BusinessDayResponse[]>>(
        `/restaurants/${restaurantId}/business-days`
    )
    return response.data
}

async function openBusinessDay(restaurantId: string) {
    const response = await apiClient<ApiGenericResponse<BusinessDayResponse>>(
        `/restaurants/${restaurantId}/business-days/open`,
        { method: "POST" }
    )
    return response.data
}

async function closeBusinessDay(restaurantId: string, businessDayId: string, notes?: string) {
    const response = await apiClient<ApiGenericResponse<BusinessDayClosureResponse>>(
        `/restaurants/${restaurantId}/business-days/${businessDayId}/close`,
        { method: "POST", body: JSON.stringify({ notes }) }
    )
    return response.data
}

async function reopenBusinessDay(restaurantId: string, businessDayId: string, reason: string) {
    const response = await apiClient<ApiGenericResponse<BusinessDayResponse>>(
        `/restaurants/${restaurantId}/business-days/${businessDayId}/reopen`,
        { method: "POST", body: JSON.stringify({ reason }) }
    )
    return response.data
}

async function movements(restaurantId: string, sessionId: string) {
    const response = await apiClient<ApiGenericResponse<CashMovementResponse[]>>(
        `/restaurants/${restaurantId}/cash-sessions/${sessionId}/movements`
    )
    return response.data
}

async function recordMovement(restaurantId: string, sessionId: string, request: CashMovementRequest) {
    const response = await apiClient<ApiGenericResponse<CashMovementResponse>>(
        `/restaurants/${restaurantId}/cash-sessions/${sessionId}/movements`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function voidMovement(restaurantId: string, sessionId: string, movementId: string, reason: string) {
    const response = await apiClient<ApiGenericResponse<CashMovementResponse>>(
        `/restaurants/${restaurantId}/cash-sessions/${sessionId}/movements/${movementId}/void`,
        { method: "POST", body: JSON.stringify({ reason }) }
    )
    return response.data
}

async function reconciliation(restaurantId: string, sessionId: string) {
    const response = await apiClient<ApiGenericResponse<CashSessionReconciliationResponse>>(
        `/restaurants/${restaurantId}/cash-sessions/${sessionId}/reconciliation`
    )
    return response.data
}

function useInvalidateCashOperations() {
    const queryClient = useQueryClient()
    return () => {
        queryClient.invalidateQueries({ queryKey: cashOperationsKeys.all })
        queryClient.invalidateQueries({ queryKey: cashSessionKeys.all })
    }
}

export function useCurrentBusinessDay() {
    const { restaurantId } = useRestaurantContext()
    return useQuery({
        queryKey: cashOperationsKeys.currentDay(restaurantId),
        queryFn: () => currentBusinessDay(restaurantId!),
        enabled: !!restaurantId,
    })
}

export function useBusinessDays() {
    const { restaurantId } = useRestaurantContext()
    return useQuery({
        queryKey: cashOperationsKeys.days(restaurantId),
        queryFn: () => businessDays(restaurantId!),
        enabled: !!restaurantId,
    })
}

export function useOpenBusinessDay() {
    const { restaurantId } = useRestaurantContext()
    const invalidate = useInvalidateCashOperations()
    return useMutation({
        mutationFn: () => openBusinessDay(restaurantId!),
        onSuccess: () => {
            invalidate()
            toast.success("Business day opened")
        },
        onError: (error: Error) => toast.error(error.message),
    })
}

export function useCloseBusinessDay() {
    const { restaurantId } = useRestaurantContext()
    const invalidate = useInvalidateCashOperations()
    return useMutation({
        mutationFn: ({ businessDayId, notes }: { businessDayId: string; notes?: string }) =>
            closeBusinessDay(restaurantId!, businessDayId, notes),
        onSuccess: () => {
            invalidate()
            toast.success("Business day closed")
        },
        onError: (error: Error) => toast.error(error.message),
    })
}

export function useReopenBusinessDay() {
    const { restaurantId } = useRestaurantContext()
    const invalidate = useInvalidateCashOperations()
    return useMutation({
        mutationFn: ({ businessDayId, reason }: { businessDayId: string; reason: string }) =>
            reopenBusinessDay(restaurantId!, businessDayId, reason),
        onSuccess: () => {
            invalidate()
            toast.success("Business day reopened")
        },
        onError: (error: Error) => toast.error(error.message),
    })
}

export function useCashMovements(sessionId: string | null) {
    const { restaurantId } = useRestaurantContext()
    return useQuery({
        queryKey: cashOperationsKeys.movements(restaurantId, sessionId),
        queryFn: () => movements(restaurantId!, sessionId!),
        enabled: !!restaurantId && !!sessionId,
    })
}

export function useRecordCashMovement(sessionId: string) {
    const { restaurantId } = useRestaurantContext()
    const invalidate = useInvalidateCashOperations()
    return useMutation({
        mutationFn: (request: CashMovementRequest) => recordMovement(restaurantId!, sessionId, request),
        onSuccess: () => {
            invalidate()
            toast.success("Cash movement recorded")
        },
        onError: (error: Error) => toast.error(error.message),
    })
}

export function useVoidCashMovement(sessionId: string) {
    const { restaurantId } = useRestaurantContext()
    const invalidate = useInvalidateCashOperations()
    return useMutation({
        mutationFn: ({ movementId, reason }: { movementId: string; reason: string }) =>
            voidMovement(restaurantId!, sessionId, movementId, reason),
        onSuccess: () => {
            invalidate()
            toast.success("Cash movement voided")
        },
        onError: (error: Error) => toast.error(error.message),
    })
}

export function useCashSessionReconciliation(
    sessionId: string | null,
    enabled = true,
    restaurantIdOverride?: string
) {
    const context = useOptionalRestaurantContext()
    const restaurantId = restaurantIdOverride ?? context?.restaurantId ?? null
    return useQuery({
        queryKey: cashOperationsKeys.reconciliation(restaurantId, sessionId),
        queryFn: () => reconciliation(restaurantId!, sessionId!),
        enabled: enabled && !!restaurantId && !!sessionId,
    })
}
