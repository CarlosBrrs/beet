import { useQuery, useMutation, useQueryClient, keepPreviousData } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import {
    ApiGenericResponse,
    PageResponse,
    InventoryStockResponse,
    ActivateIngredientInventoryRequest,
    AdjustStockRequest,
    InventoryTransactionResponse,
} from "@/lib/api-types"

// ── Query Key Factory ──

export interface InventoryListParams {
    restaurantId: string
    page: number
    size: number
    search?: string
    sortBy?: string
    sortDesc?: boolean
}


export interface TransactionListParams {
    restaurantId: string
    stockId: string
    page: number
    size: number
}

export const InventoryKeys = {
    all: (rid: string) => ["inventory", rid] as const,
    stocks: (rid: string) => [...InventoryKeys.all(rid), "stocks"] as const,
    stockList: (params: InventoryListParams) => [...InventoryKeys.stocks(params.restaurantId), params] as const,
    available: (rid: string) => [...InventoryKeys.all(rid), "available"] as const,
    transactions: (rid: string, stockId: string) => [...InventoryKeys.all(rid), "transactions", stockId] as const,
    transactionList: (params: TransactionListParams) => [...InventoryKeys.transactions(params.restaurantId, params.stockId), params] as const,
}

// ── Inventory Stocks (paginated) ──

async function fetchInventoryStocks(params: InventoryListParams): Promise<PageResponse<InventoryStockResponse>> {
    const sp = new URLSearchParams()
    sp.append("page", params.page.toString())
    sp.append("size", params.size.toString())
    if (params.search) sp.append("search", params.search)
    if (params.sortBy) sp.append("sortBy", params.sortBy)
    if (params.sortDesc !== undefined) sp.append("sortDesc", params.sortDesc.toString())

    const data = await apiClient<ApiGenericResponse<PageResponse<InventoryStockResponse>>>(
        `/restaurants/${params.restaurantId}/inventory?${sp.toString()}`
    )
    if (!data.success) throw new Error(data.errorMessage || "Failed to fetch inventory stocks")
    return data.data
}

export function useInventoryStocks(params: InventoryListParams) {
    return useQuery({
        queryKey: InventoryKeys.stockList(params),
        queryFn: () => fetchInventoryStocks(params),
        placeholderData: keepPreviousData,
        enabled: !!params.restaurantId,
    })
}

// ── Available Ingredients (list — not yet activated) ──

async function fetchAvailableIngredients(restaurantId: string): Promise<InventoryStockResponse[]> {
    const data = await apiClient<ApiGenericResponse<InventoryStockResponse[]>>(
        `/restaurants/${restaurantId}/inventory/available`
    )
    if (!data.success) throw new Error(data.errorMessage || "Failed to fetch available ingredients")
    return data.data
}

export function useAvailableIngredients(restaurantId: string) {
    return useQuery({
        queryKey: InventoryKeys.available(restaurantId),
        queryFn: () => fetchAvailableIngredients(restaurantId),
        enabled: !!restaurantId,
    })
}

// ── Activate Ingredient ──

async function activateIngredient(
    restaurantId: string,
    payload: ActivateIngredientInventoryRequest
): Promise<InventoryStockResponse> {
    const data = await apiClient<ApiGenericResponse<InventoryStockResponse>>(
        `/restaurants/${restaurantId}/inventory/activate`,
        { method: "POST", body: JSON.stringify(payload) }
    )
    if (!data.success) throw new Error(data.errorMessage || "Failed to activate ingredient")
    return data.data
}

export function useActivateIngredient(restaurantId: string) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (payload: ActivateIngredientInventoryRequest) =>
            activateIngredient(restaurantId, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: InventoryKeys.stocks(restaurantId) })
            queryClient.invalidateQueries({ queryKey: InventoryKeys.available(restaurantId) })
        },
    })
}

// ── Adjust Stock ──

async function adjustStock(
    restaurantId: string,
    stockId: string,
    payload: AdjustStockRequest
): Promise<InventoryTransactionResponse> {
    const data = await apiClient<ApiGenericResponse<InventoryTransactionResponse>>(
        `/restaurants/${restaurantId}/inventory/${stockId}/adjust`,
        { method: "PUT", body: JSON.stringify(payload) }
    )
    if (!data.success) throw new Error(data.errorMessage || "Failed to adjust stock")
    return data.data
}

export function useAdjustInventoryStock(restaurantId: string) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ stockId, payload }: { stockId: string; payload: AdjustStockRequest }) =>
            adjustStock(restaurantId, stockId, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: InventoryKeys.stocks(restaurantId) })
        },
    })
}

// ── Transaction History (paginated) ──

async function fetchTransactions(params: TransactionListParams): Promise<PageResponse<InventoryTransactionResponse>> {
    const sp = new URLSearchParams()
    sp.append("page", params.page.toString())
    sp.append("size", params.size.toString())

    const data = await apiClient<ApiGenericResponse<PageResponse<InventoryTransactionResponse>>>(
        `/restaurants/${params.restaurantId}/inventory/${params.stockId}/transactions?${sp.toString()}`
    )
    if (!data.success) throw new Error(data.errorMessage || "Failed to fetch transactions")
    return data.data
}

export function useTransactions(params: TransactionListParams) {
    return useQuery({
        queryKey: InventoryKeys.transactionList(params),
        queryFn: () => fetchTransactions(params),
        placeholderData: keepPreviousData,
        enabled: !!params.restaurantId && !!params.stockId,
    })
}
