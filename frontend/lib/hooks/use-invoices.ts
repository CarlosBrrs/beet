import { useQuery, useMutation, useQueryClient, keepPreviousData } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import {
    ApiGenericResponse,
    PageResponse,
    InvoiceResponse,
    InvoiceDetailResponse,
    RegisterInvoiceRequest,
    SupplierItemForInvoiceResponse,
} from "@/lib/api-types"

// ── Query Key Factory ──

export interface InvoiceListParams {
    restaurantId: string
    page: number
    size: number
    search?: string
}

const invoiceKeys = {
    all: ["invoices"] as const,
    list: (params: InvoiceListParams) => [...invoiceKeys.all, "list", params] as const,
    detail: (restaurantId: string, invoiceId: string) => [...invoiceKeys.all, "detail", restaurantId, invoiceId] as const,
    supplierItems: (restaurantId: string, supplierId: string) => [...invoiceKeys.all, "supplierItems", restaurantId, supplierId] as const,
}

// ── Fetch Functions ──

async function fetchInvoices(params: InvoiceListParams): Promise<PageResponse<InvoiceResponse>> {
    const sp = new URLSearchParams()
    sp.append("page", String(params.page))
    sp.append("size", String(params.size))
    if (params.search) sp.append("search", params.search)

    const data = await apiClient<ApiGenericResponse<PageResponse<InvoiceResponse>>>(
        `/restaurants/${params.restaurantId}/invoices?${sp}`,
        { method: "GET" }
    )
    return data.data
}

async function fetchInvoiceDetail(restaurantId: string, invoiceId: string): Promise<InvoiceDetailResponse> {
    const data = await apiClient<ApiGenericResponse<InvoiceDetailResponse>>(
        `/restaurants/${restaurantId}/invoices/${invoiceId}`,
        { method: "GET" }
    )
    return data.data
}

async function fetchSupplierItems(restaurantId: string, supplierId: string): Promise<SupplierItemForInvoiceResponse[]> {
    const data = await apiClient<ApiGenericResponse<SupplierItemForInvoiceResponse[]>>(
        `/restaurants/${restaurantId}/suppliers/${supplierId}/items`,
        { method: "GET" }
    )
    return data.data
}

async function registerInvoice(restaurantId: string, request: RegisterInvoiceRequest): Promise<InvoiceResponse> {
    const data = await apiClient<ApiGenericResponse<InvoiceResponse>>(
        `/restaurants/${restaurantId}/invoices`,
        { method: "POST", body: JSON.stringify(request) }
    )
    return data.data
}

// ── Hooks ──

export function useInvoices(params: InvoiceListParams) {
    return useQuery({
        queryKey: invoiceKeys.list(params),
        queryFn: () => fetchInvoices(params),
        placeholderData: keepPreviousData,
        enabled: !!params.restaurantId,
    })
}

export function useInvoiceDetail(restaurantId: string, invoiceId: string) {
    return useQuery({
        queryKey: invoiceKeys.detail(restaurantId, invoiceId),
        queryFn: () => fetchInvoiceDetail(restaurantId, invoiceId),
        enabled: !!restaurantId && !!invoiceId,
    })
}

export function useSupplierItems(restaurantId: string, supplierId: string | null) {
    return useQuery({
        queryKey: invoiceKeys.supplierItems(restaurantId, supplierId ?? ""),
        queryFn: () => fetchSupplierItems(restaurantId, supplierId!),
        enabled: !!restaurantId && !!supplierId,
    })
}

export function useRegisterInvoice(restaurantId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (request: RegisterInvoiceRequest) => registerInvoice(restaurantId, request),
        onSuccess: () => {
            // Invalidate both invoices and inventory (stock changed)
            queryClient.invalidateQueries({ queryKey: invoiceKeys.all })
            queryClient.invalidateQueries({ queryKey: ["inventory"] })
        },
    })
}
