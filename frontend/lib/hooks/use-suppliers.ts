import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import { ApiGenericResponse, PageResponse, SupplierResponse } from "@/lib/api-types"

export interface SupplierListParams {
    page: number
    size: number
    search?: string
    active?: boolean
    sortBy?: string
    sortDesc?: boolean
}

export interface SupplierRequest {
    name: string
    documentTypeId: string
    documentNumber: string
    contactName?: string | null
    email?: string | null
    phone?: string | null
    address?: string | null
}

export const SupplierKeys = {
    all: () => ["suppliers"] as const,
    lists: () => [...SupplierKeys.all(), "list"] as const,
    list: (params: SupplierListParams) => [...SupplierKeys.lists(), params] as const,
    active: () => [...SupplierKeys.all(), "active"] as const,
    details: () => [...SupplierKeys.all(), "detail"] as const,
    detail: (id: string) => [...SupplierKeys.details(), id] as const,
}

async function fetchSuppliers(params: SupplierListParams): Promise<PageResponse<SupplierResponse>> {
    const searchParams = new URLSearchParams()
    searchParams.set("page", params.page.toString())
    searchParams.set("size", params.size.toString())
    if (params.search) searchParams.set("search", params.search)
    if (params.active !== undefined) searchParams.set("active", String(params.active))
    if (params.sortBy) searchParams.set("sortBy", params.sortBy)
    if (params.sortDesc !== undefined) searchParams.set("sortDesc", String(params.sortDesc))

    const response = await apiClient<ApiGenericResponse<PageResponse<SupplierResponse>>>(`/suppliers?${searchParams.toString()}`)
    if (!response.success) throw new Error(response.errorMessage || "Failed to fetch suppliers")
    return response.data
}

async function fetchActiveSuppliers(): Promise<SupplierResponse[]> {
    const response = await apiClient<ApiGenericResponse<SupplierResponse[]>>("/suppliers?unpagedActive=true")
    if (!response.success) throw new Error(response.errorMessage || "Failed to fetch suppliers")
    return response.data
}

async function fetchSupplier(id: string): Promise<SupplierResponse> {
    const response = await apiClient<ApiGenericResponse<SupplierResponse>>(`/suppliers/${id}`)
    if (!response.success) throw new Error(response.errorMessage || "Failed to fetch supplier")
    return response.data
}

async function createSupplier(payload: SupplierRequest): Promise<SupplierResponse> {
    const response = await apiClient<ApiGenericResponse<SupplierResponse>>("/suppliers", {
        method: "POST",
        body: JSON.stringify(payload),
    })
    if (!response.success) throw new Error(response.errorMessage || "Failed to create supplier")
    return response.data
}

async function updateSupplier(id: string, payload: SupplierRequest): Promise<SupplierResponse> {
    const response = await apiClient<ApiGenericResponse<SupplierResponse>>(`/suppliers/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
    })
    if (!response.success) throw new Error(response.errorMessage || "Failed to update supplier")
    return response.data
}

async function setSupplierActive(id: string, isActive: boolean): Promise<SupplierResponse> {
    const response = await apiClient<ApiGenericResponse<SupplierResponse>>(`/suppliers/${id}/activation`, {
        method: "PATCH",
        body: JSON.stringify({ isActive }),
    })
    if (!response.success) throw new Error(response.errorMessage || "Failed to update supplier status")
    return response.data
}

async function deleteSupplier(id: string): Promise<void> {
    const response = await apiClient<ApiGenericResponse<void>>(`/suppliers/${id}`, { method: "DELETE" })
    if (!response.success) throw new Error(response.errorMessage || "Failed to delete supplier")
}

export function useSupplierList(params: SupplierListParams) {
    return useQuery({
        queryKey: SupplierKeys.list(params),
        queryFn: () => fetchSuppliers(params),
        placeholderData: keepPreviousData,
    })
}

export function useSuppliers() {
    return useQuery({
        queryKey: SupplierKeys.active(),
        queryFn: fetchActiveSuppliers,
        staleTime: 1000 * 60 * 5,
    })
}

export function useSupplier(id?: string) {
    return useQuery({
        queryKey: SupplierKeys.detail(id ?? ""),
        queryFn: () => fetchSupplier(id as string),
        enabled: !!id,
    })
}

export function useCreateSupplier() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: createSupplier,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: SupplierKeys.all() })
        },
    })
}

export function useUpdateSupplier() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ id, payload }: { id: string; payload: SupplierRequest }) => updateSupplier(id, payload),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: SupplierKeys.all() })
            queryClient.invalidateQueries({ queryKey: SupplierKeys.detail(variables.id) })
        },
    })
}

export function useSetSupplierActive() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) => setSupplierActive(id, isActive),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: SupplierKeys.all() })
            queryClient.invalidateQueries({ queryKey: SupplierKeys.detail(variables.id) })
        },
    })
}

export function useDeleteSupplier() {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: deleteSupplier,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: SupplierKeys.all() })
        },
    })
}
