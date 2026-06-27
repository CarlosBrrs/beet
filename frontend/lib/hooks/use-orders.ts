"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useEffect, useRef, useState } from "react"
import { toast } from "sonner"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { apiClient } from "@/lib/api-client"
import { getOrCreateDeviceId } from "@/lib/device-id"
import { env } from "@/lib/env"
import {
    ApiGenericResponse,
    BillPaymentStatus,
    CancelOrderItemRequest,
    CancelOrderRequest,
    KitchenTicketResponse,
    KitchenTicketStatus,
    KitchenTicketStreamEvent,
    OrderBillResponse,
    OrderCreateRequest,
    OrderDetailResponse,
    OrderItemRequest,
    OrderResponse,
    OrderStatus,
    PageResponse,
    PaymentMethodRequest,
    PaymentMethodResponse,
    PaymentRequest,
    PaymentResponse,
    PaymentRefundRequest,
    PaymentRefundResponse,
    PaymentPendingState,
    PosCatalogResponse,
    ServiceType,
    SplitBillsRequest,
} from "@/lib/api-types"

export const orderKeys = {
    all: ["orders"] as const,
    posCatalog: (restaurantId: string | null, params: PosCatalogParams) =>
        [...orderKeys.all, "pos-catalog", restaurantId, params] as const,
    list: (restaurantId: string | null, params: OrderListParams) =>
        [...orderKeys.all, "list", restaurantId, params] as const,
    detail: (restaurantId: string | null, orderId: string | null) =>
        [...orderKeys.all, "detail", restaurantId, orderId] as const,
    kitchenTickets: (restaurantId: string | null, params: KitchenTicketListParams) =>
        [...orderKeys.all, "kitchen-tickets", restaurantId, params] as const,
    paymentMethods: (restaurantId: string | null) =>
        [...orderKeys.all, "payment-methods", restaurantId] as const,
    refunds: (restaurantId: string | null, orderId: string | null) =>
        [...orderKeys.all, "refunds", restaurantId, orderId] as const,
    bills: (restaurantId: string | null, orderId: string | null, params: BillListParams) =>
        [...orderKeys.all, "bills", restaurantId, orderId, params] as const,
}

export interface PosCatalogParams {
    page?: number
    size?: number
    search?: string
    menuId?: string
    submenuId?: string
    referenceType?: "PRODUCT" | "TEMPLATE"
    sort?: string
}

export interface OrderListParams {
    page?: number
    size?: number
    sort?: string
    orderStatus?: OrderStatus
    serviceType?: ServiceType
    search?: string
    paymentPendingState?: PaymentPendingState
}

export interface KitchenTicketListParams {
    page?: number
    size?: number
    status?: KitchenTicketStatus
}

export interface BillListParams {
    page?: number
    size?: number
    paymentStatus?: BillPaymentStatus
    search?: string
    sort?: string
}


interface KitchenTicketStreamOptions {
    enabled?: boolean
    onTicketCreated?: (event: KitchenTicketStreamEvent) => void
    onTicketEvent?: (event: KitchenTicketStreamEvent) => void
}

function parseSseMessage(message: string) {
    const lines = message.split(/\r?\n/)
    const eventName = lines.find((line) => line.startsWith("event:"))?.slice(6).trim() ?? "message"
    const data = lines
        .filter((line) => line.startsWith("data:"))
        .map((line) => line.slice(5).trimStart())
        .join("\n")
    return { eventName, data }
}

function isKitchenTicketStreamEvent(value: unknown): value is KitchenTicketStreamEvent {
    if (!value || typeof value !== "object") return false
    const candidate = value as Partial<KitchenTicketStreamEvent>
    return typeof candidate.eventType === "string"
        && typeof candidate.restaurantId === "string"
        && typeof candidate.ticketId === "string"
        && typeof candidate.orderId === "string"
        && typeof candidate.status === "string"
        && typeof candidate.occurredAt === "string"
}
function withParams(path: string, params: object) {
    const search = new URLSearchParams()
    Object.entries(params as Record<string, string | number | undefined>).forEach(([key, value]) => {
        if (value !== undefined && value !== "") search.set(key, String(value))
    })
    const query = search.toString()
    return query ? `${path}?${query}` : path
}

async function fetchPosCatalog(restaurantId: string, params: PosCatalogParams) {
    const response = await apiClient<ApiGenericResponse<PageResponse<PosCatalogResponse>>>(
        withParams(`/restaurants/${restaurantId}/pos/catalog`, params)
    )
    return response.data
}

async function createDraftOrder(restaurantId: string, request: OrderCreateRequest) {
    const response = await apiClient<ApiGenericResponse<OrderDetailResponse>>(
        `/restaurants/${restaurantId}/orders/draft`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function confirmOrder(restaurantId: string, orderId: string) {
    const response = await apiClient<ApiGenericResponse<OrderDetailResponse>>(
        `/restaurants/${restaurantId}/orders/${orderId}/confirm`,
        { method: "POST" }
    )
    return response.data
}

async function completeOrder(restaurantId: string, orderId: string) {
    const response = await apiClient<ApiGenericResponse<OrderDetailResponse>>(
        `/restaurants/${restaurantId}/orders/${orderId}/complete`,
        { method: "POST" }
    )
    return response.data
}

async function cancelOrder(restaurantId: string, orderId: string, request: CancelOrderRequest) {
    const response = await apiClient<ApiGenericResponse<OrderDetailResponse>>(
        `/restaurants/${restaurantId}/orders/${orderId}/cancel`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function cancelOrderItem(
    restaurantId: string,
    orderId: string,
    orderItemId: string,
    request: CancelOrderItemRequest
) {
    const response = await apiClient<ApiGenericResponse<OrderDetailResponse>>(
        `/restaurants/${restaurantId}/orders/${orderId}/items/${orderItemId}/cancel`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function addOrderItems(restaurantId: string, orderId: string, items: OrderItemRequest[]) {
    const response = await apiClient<ApiGenericResponse<OrderDetailResponse>>(
        `/restaurants/${restaurantId}/orders/${orderId}/items/batch`,
        { method: "POST", body: JSON.stringify({ items }) }
    )
    return response.data
}

async function reactivatePayment(restaurantId: string, orderId: string) {
    const response = await apiClient<ApiGenericResponse<OrderDetailResponse>>(
        `/restaurants/${restaurantId}/orders/${orderId}/payment-reactivation`,
        { method: "POST" }
    )
    return response.data
}

async function fetchOrders(restaurantId: string, params: OrderListParams) {
    const response = await apiClient<ApiGenericResponse<PageResponse<OrderResponse>>>(
        withParams(`/restaurants/${restaurantId}/orders`, params)
    )
    return response.data
}

async function fetchOrderDetail(restaurantId: string, orderId: string) {
    const response = await apiClient<ApiGenericResponse<OrderDetailResponse>>(
        `/restaurants/${restaurantId}/orders/${orderId}`
    )
    return response.data
}

async function fetchKitchenTickets(restaurantId: string, params: KitchenTicketListParams) {
    const response = await apiClient<ApiGenericResponse<PageResponse<KitchenTicketResponse>>>(
        withParams(`/restaurants/${restaurantId}/kitchen-tickets`, params)
    )
    return response.data
}

async function updateKitchenTicketStatus(restaurantId: string, ticketId: string, status: KitchenTicketStatus) {
    const response = await apiClient<ApiGenericResponse<KitchenTicketResponse>>(
        `/restaurants/${restaurantId}/kitchen-tickets/${ticketId}/status`,
        { method: "PATCH", body: JSON.stringify({ status }) }
    )
    return response.data
}

async function fetchPaymentMethods(restaurantId: string) {
    const response = await apiClient<ApiGenericResponse<PaymentMethodResponse[]>>(
        `/restaurants/${restaurantId}/payment-methods`
    )
    return response.data
}

async function createPaymentMethod(restaurantId: string, request: PaymentMethodRequest) {
    const response = await apiClient<ApiGenericResponse<PaymentMethodResponse>>(
        `/restaurants/${restaurantId}/payment-methods`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function updatePaymentMethod(restaurantId: string, methodId: string, request: PaymentMethodRequest) {
    const response = await apiClient<ApiGenericResponse<PaymentMethodResponse>>(
        `/restaurants/${restaurantId}/payment-methods/${methodId}`,
        { method: "PATCH", body: JSON.stringify(request) }
    )
    return response.data
}

async function registerPayment(restaurantId: string, orderId: string, request: PaymentRequest) {
    const response = await apiClient<ApiGenericResponse<PaymentResponse>>(
        `/restaurants/${restaurantId}/orders/${orderId}/payments`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function fetchRefunds(restaurantId: string, orderId: string) {
    const response = await apiClient<ApiGenericResponse<PaymentRefundResponse[]>>(
        `/restaurants/${restaurantId}/orders/${orderId}/refunds`
    )
    return response.data
}

async function registerRefund(restaurantId: string, orderId: string, request: PaymentRefundRequest) {
    const response = await apiClient<ApiGenericResponse<PaymentRefundResponse>>(
        `/restaurants/${restaurantId}/orders/${orderId}/refunds`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function splitBills(restaurantId: string, orderId: string, request: SplitBillsRequest) {
    const response = await apiClient<ApiGenericResponse<OrderBillResponse[]>>(
        `/restaurants/${restaurantId}/orders/${orderId}/bills/split`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return response.data
}

async function fetchBills(restaurantId: string, orderId: string, params: BillListParams) {
    const response = await apiClient<ApiGenericResponse<PageResponse<OrderBillResponse>>>(
        withParams(`/restaurants/${restaurantId}/orders/${orderId}/bills`, params)
    )
    return response.data
}

export function usePosCatalog(params: PosCatalogParams) {
    const { restaurantId } = useRestaurantContext()
    return useQuery({
        queryKey: orderKeys.posCatalog(restaurantId, params),
        queryFn: () => fetchPosCatalog(restaurantId!, params),
        enabled: !!restaurantId,
    })
}

export function useCreateDraftOrder() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (request: OrderCreateRequest) => createDraftOrder(restaurantId!, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            toast.success("Order draft created")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to create order"),
    })
}

export function useConfirmOrder() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (orderId: string) => confirmOrder(restaurantId!, orderId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            toast.success("Order confirmed")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to confirm order"),
    })
}

export function useCompleteOrder() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (orderId: string) => completeOrder(restaurantId!, orderId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            toast.success("Order completed")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to complete order"),
    })
}

export function useCancelOrder() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ orderId, request }: { orderId: string; request: CancelOrderRequest }) =>
            cancelOrder(restaurantId!, orderId, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            toast.success("Order canceled")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to cancel order"),
    })
}

export function useCancelOrderItem(orderId: string | null) {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ orderItemId, request }: { orderItemId: string; request: CancelOrderItemRequest }) =>
            cancelOrderItem(restaurantId!, orderId!, orderItemId, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            toast.success("Order item canceled")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to cancel order item"),
    })
}

export function useAddOrderItems(orderId: string | null) {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (items: OrderItemRequest[]) => addOrderItems(restaurantId!, orderId!, items),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            toast.success("Items agregados a la orden")
        },
        onError: (error: Error) => toast.error(error.message || "No se pudieron agregar los items"),
    })
}

export function useReactivatePayment() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (orderId: string) => reactivatePayment(restaurantId!, orderId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            toast.success("Orden reactivada y stock reservado")
        },
        onError: (error: Error) => toast.error(error.message || "No se pudo reactivar la orden"),
    })
}

export function useOrders(params: OrderListParams) {
    const { restaurantId } = useRestaurantContext()
    return useQuery({
        queryKey: orderKeys.list(restaurantId, params),
        queryFn: () => fetchOrders(restaurantId!, params),
        enabled: !!restaurantId,
    })
}

export function useOrderDetail(orderId: string | null) {
    const { restaurantId } = useRestaurantContext()
    return useQuery({
        queryKey: orderKeys.detail(restaurantId, orderId),
        queryFn: () => fetchOrderDetail(restaurantId!, orderId!),
        enabled: !!restaurantId && !!orderId,
    })
}

export function useKitchenTickets(params: KitchenTicketListParams) {
    const { restaurantId } = useRestaurantContext()
    return useQuery({
        queryKey: orderKeys.kitchenTickets(restaurantId, params),
        queryFn: () => fetchKitchenTickets(restaurantId!, params),
        enabled: !!restaurantId,
    })
}


export function useKitchenTicketStream(options: KitchenTicketStreamOptions = {}) {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    const [isConnected, setIsConnected] = useState(false)
    const onTicketCreatedRef = useRef(options.onTicketCreated)
    const onTicketEventRef = useRef(options.onTicketEvent)
    const enabled = options.enabled ?? true

    useEffect(() => {
        onTicketCreatedRef.current = options.onTicketCreated
        onTicketEventRef.current = options.onTicketEvent
    }, [options.onTicketCreated, options.onTicketEvent])

    useEffect(() => {
        if (!enabled || !restaurantId) return undefined

        const controller = new AbortController()
        let reconnectTimer: number | undefined
        let closed = false

        const handleEvent = (rawMessage: string) => {
            const { eventName, data } = parseSseMessage(rawMessage)
            if (!data || eventName === "kitchen.heartbeat") return

            let parsed: unknown
            try {
                parsed = JSON.parse(data)
            } catch {
                return
            }
            if (!isKitchenTicketStreamEvent(parsed)) return

            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            onTicketEventRef.current?.(parsed)
            if (parsed.eventType === "kitchen.ticket.created") {
                onTicketCreatedRef.current?.(parsed)
            }
        }

        const connect = async () => {
            const token = localStorage.getItem("beet_token")
            try {
                const response = await fetch(`${env.NEXT_PUBLIC_API_URL}/restaurants/${restaurantId}/kitchen-tickets/stream`, {
                    headers: {
                        "Accept": "text/event-stream",
                        "X-Device-Id": getOrCreateDeviceId(),
                        ...(token ? { Authorization: `Bearer ${token}` } : {}),
                    },
                    signal: controller.signal,
                })
                if (!response.ok || !response.body) {
                    throw new Error("KDS stream unavailable")
                }

                setIsConnected(true)
                const reader = response.body.getReader()
                const decoder = new TextDecoder()
                let buffer = ""

                while (!closed) {
                    const { value, done } = await reader.read()
                    if (done) break
                    buffer += decoder.decode(value, { stream: true })
                    const messages = buffer.split(/\r?\n\r?\n/)
                    buffer = messages.pop() ?? ""
                    messages.forEach(handleEvent)
                }
            } catch {
                if (!closed) {
                    setIsConnected(false)
                    reconnectTimer = window.setTimeout(connect, 3000)
                }
            }
        }

        void connect()

        return () => {
            closed = true
            setIsConnected(false)
            controller.abort()
            if (reconnectTimer) window.clearTimeout(reconnectTimer)
        }
    }, [enabled, queryClient, restaurantId])

    return { isConnected }
}
export function useUpdateKitchenTicketStatus() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ ticketId, status }: { ticketId: string; status: KitchenTicketStatus }) =>
            updateKitchenTicketStatus(restaurantId!, ticketId, status),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            toast.success("Kitchen ticket updated")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to update kitchen ticket"),
    })
}

export function usePaymentMethods() {
    const { restaurantId } = useRestaurantContext()
    return useQuery({
        queryKey: orderKeys.paymentMethods(restaurantId),
        queryFn: () => fetchPaymentMethods(restaurantId!),
        enabled: !!restaurantId,
    })
}

export function useCreatePaymentMethod() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (request: PaymentMethodRequest) => createPaymentMethod(restaurantId!, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.paymentMethods(restaurantId) })
            toast.success("Payment method created")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to create payment method"),
    })
}

export function useUpdatePaymentMethod() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ methodId, request }: { methodId: string; request: PaymentMethodRequest }) =>
            updatePaymentMethod(restaurantId!, methodId, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.paymentMethods(restaurantId) })
            toast.success("Payment method updated")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to update payment method"),
    })
}

export function useRegisterPayment() {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ orderId, request }: { orderId: string; request: PaymentRequest }) =>
            registerPayment(restaurantId!, orderId, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            toast.success("Payment registered")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to register payment"),
    })
}

export function useRefunds(orderId: string | null) {
    const { restaurantId } = useRestaurantContext()
    return useQuery({
        queryKey: orderKeys.refunds(restaurantId, orderId),
        queryFn: () => fetchRefunds(restaurantId!, orderId!),
        enabled: !!restaurantId && !!orderId,
    })
}

export function useRegisterRefund(orderId: string | null) {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (request: PaymentRefundRequest) => registerRefund(restaurantId!, orderId!, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.all })
            toast.success("Refund registered")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to register refund"),
    })
}

export function useSplitBills(orderId: string | null) {
    const { restaurantId } = useRestaurantContext()
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (request: SplitBillsRequest) => splitBills(restaurantId!, orderId!, request),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: orderKeys.bills(restaurantId, orderId, {}) })
            toast.success("Bills created")
        },
        onError: (error: Error) => toast.error(error.message || "Failed to split bills"),
    })
}

export function useBills(orderId: string | null, params: BillListParams) {
    const { restaurantId } = useRestaurantContext()
    return useQuery({
        queryKey: orderKeys.bills(restaurantId, orderId, params),
        queryFn: () => fetchBills(restaurantId!, orderId!, params),
        enabled: !!restaurantId && !!orderId,
    })
}
