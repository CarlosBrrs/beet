import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { ApiClientError, apiClient } from "@/lib/api-client"
import {
    ApiGenericResponse,
    CashSessionListResponse,
    CashSessionResponse,
    CloseCashSessionRequest,
    OpenCashSessionRequest,
    PageResponse,
} from "@/lib/api-types"
import { cashRegisterKeys } from "@/lib/hooks/use-cash-registers"

export const cashSessionKeys = {
    all: ["cash-sessions"] as const,
    active: (restaurantId: string | null) => [...cashSessionKeys.all, "active", restaurantId] as const,
    lists: () => [...cashSessionKeys.all, "list"] as const,
    list: (params: CashSessionListParams) => [...cashSessionKeys.lists(), params] as const,
}

export interface CashSessionListParams {
    scope: "restaurant" | "account"
    restaurantId?: string
    status?: "OPEN" | "CLOSED"
    from?: string
    to?: string
    cashRegisterId?: string
    page: number
    size: number
}

async function fetchActiveCashSession(restaurantId: string): Promise<CashSessionResponse | null> {
    try {
        const response = await apiClient<ApiGenericResponse<CashSessionResponse | null>>(
            `/restaurants/${restaurantId}/cash-sessions/active`
        )
        return response.data
    } catch (error) {
        if (error instanceof ApiClientError && error.status === 404) {
            return null
        }
        throw error
    }
}

async function openCashSession(
    restaurantId: string,
    request: OpenCashSessionRequest
): Promise<CashSessionResponse> {
    const response = await apiClient<ApiGenericResponse<CashSessionResponse>>(
        `/restaurants/${restaurantId}/cash-sessions/open`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function closeCashSession(
    restaurantId: string,
    sessionId: string,
    request: CloseCashSessionRequest
): Promise<CashSessionResponse> {
    const response = await apiClient<ApiGenericResponse<CashSessionResponse>>(
        `/restaurants/${restaurantId}/cash-sessions/${sessionId}/close`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function fetchCashSessions(params: CashSessionListParams): Promise<PageResponse<CashSessionListResponse>> {
    const searchParams = new URLSearchParams()
    searchParams.set("timeZone", Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC")
    if (params.scope === "account" && params.restaurantId) searchParams.set("restaurantId", params.restaurantId)
    if (params.status) searchParams.set("status", params.status)
    if (params.from) searchParams.set("from", params.from)
    if (params.to) searchParams.set("to", params.to)
    if (params.cashRegisterId) searchParams.set("cashRegisterId", params.cashRegisterId)
    searchParams.set("page", String(params.page))
    searchParams.set("size", String(params.size))

    const endpoint = params.scope === "account"
        ? `/account/cash-sessions?${searchParams}`
        : `/restaurants/${params.restaurantId}/cash-sessions?${searchParams}`
    const response = await apiClient<ApiGenericResponse<PageResponse<CashSessionListResponse>>>(endpoint)
    return response.data
}

async function forceCloseCashSession(
    restaurantId: string,
    sessionId: string,
    request: CloseCashSessionRequest
): Promise<CashSessionResponse> {
    const response = await apiClient<ApiGenericResponse<CashSessionResponse>>(
        `/restaurants/${restaurantId}/cash-sessions/${sessionId}/force-close`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

export function useActiveCashSession() {
    const { restaurantId } = useRestaurantContext()

    return useQuery({
        queryKey: cashSessionKeys.active(restaurantId),
        queryFn: () => fetchActiveCashSession(restaurantId!),
        enabled: !!restaurantId,
    })
}

export function useOpenCashSession() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (request: OpenCashSessionRequest) => openCashSession(restaurantId!, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: cashSessionKeys.active(restaurantId) })
            queryClient.invalidateQueries({ queryKey: cashSessionKeys.lists() })
            queryClient.invalidateQueries({ queryKey: cashRegisterKeys.list(restaurantId) })
            toast.success("Cash session opened")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to open cash session"),
    })
}

export function useCloseCashSession() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ sessionId, request }: { sessionId: string; request: CloseCashSessionRequest }) =>
            closeCashSession(restaurantId!, sessionId, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: cashSessionKeys.active(restaurantId) })
            queryClient.invalidateQueries({ queryKey: cashSessionKeys.lists() })
            queryClient.invalidateQueries({ queryKey: cashRegisterKeys.list(restaurantId) })
            toast.success("Cash session closed")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to close cash session"),
    })
}

export function useCashSessionList(params: CashSessionListParams) {
    return useQuery({
        queryKey: cashSessionKeys.list(params),
        queryFn: () => fetchCashSessions(params),
        placeholderData: keepPreviousData,
        enabled: params.scope === "account" || !!params.restaurantId,
    })
}

export function useForceCloseCashSession() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({
            restaurantId,
            sessionId,
            request,
        }: {
            restaurantId: string
            sessionId: string
            request: CloseCashSessionRequest
        }) => forceCloseCashSession(restaurantId, sessionId, request),
        onSuccess: (_response, variables) => {
            queryClient.invalidateQueries({ queryKey: cashSessionKeys.lists() })
            queryClient.invalidateQueries({ queryKey: cashSessionKeys.active(variables.restaurantId) })
            queryClient.invalidateQueries({ queryKey: cashRegisterKeys.list(variables.restaurantId) })
            toast.success("Cash session force closed")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to force close cash session"),
    })
}
