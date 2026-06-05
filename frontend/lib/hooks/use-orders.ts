"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { apiClient } from "@/lib/api-client"
import {
    ApiGenericResponse,
    BillPaymentStatus,
    KitchenTicketResponse,
    KitchenTicketStatus,
    OrderBillResponse,
    OrderCreateRequest,
    OrderDetailResponse,
    OrderResponse,
    OrderStatus,
    PageResponse,
    PaymentMethodRequest,
    PaymentMethodResponse,
    PaymentRequest,
    PaymentResponse,
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
    bills: (restaurantId: string | null, orderId: string | null, params: BillListParams) =>
        [...orderKeys.all, "bills", restaurantId, orderId, params] as const,
}

export interface PosCatalogParams {
    page?: number
    size?: number
    search?: string
    menuId?: string
    submenuId?: string
    availability?: string
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
