import { useQuery, useMutation, useQueryClient, keepPreviousData } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import {
    ApiGenericResponse,
    PageResponse,
    CreateIngredientRequest,
    IngredientResponse,
    IngredientListResponse,
    IngredientDetailResponse
} from "@/lib/api-types"
import { SupplierKeys, useSuppliers } from "@/lib/hooks/use-suppliers"

export { useSuppliers }

export interface IngredientListParams {
    page: number
    size: number
    search?: string
    sortBy?: string
    sortDesc?: boolean
    units?: string[]
}

export interface UpdateIngredientRequest {
    name: string
    baseUnitId: string
}

export const IngredientsKeys = {
    all: () => ["ingredients"] as const,
    lists: () => [...IngredientsKeys.all(), "list"] as const,
    list: (params: IngredientListParams) => [...IngredientsKeys.lists(), params] as const,
    details: () => [...IngredientsKeys.all(), "detail"] as const,
    detail: (id: string) => [...IngredientsKeys.details(), id] as const,
}

async function fetchIngredients(params: IngredientListParams): Promise<PageResponse<IngredientListResponse>> {
    const searchParams = new URLSearchParams()
    searchParams.append("page", params.page.toString())
    searchParams.append("size", params.size.toString())
    if (params.search) searchParams.append("search", params.search)
    if (params.sortBy) searchParams.append("sortBy", params.sortBy)
    if (params.sortDesc !== undefined) searchParams.append("sortDesc", params.sortDesc.toString())
    if (params.units) {
        params.units.forEach(unit => searchParams.append("unit", unit))
    }

    const data = await apiClient<ApiGenericResponse<PageResponse<IngredientListResponse>>>(
        `/ingredients?${searchParams.toString()}`
    )
    if (!data.success) throw new Error(data.errorMessage || "Failed to fetch ingredients")
    return data.data
}

export function useIngredients(params: IngredientListParams) {
    return useQuery({
        queryKey: IngredientsKeys.list(params),
        queryFn: () => fetchIngredients(params),
        placeholderData: keepPreviousData,
    })
}

async function fetchIngredient(ingredientId: string): Promise<IngredientDetailResponse> {
    const data = await apiClient<ApiGenericResponse<IngredientDetailResponse>>(`/ingredients/${ingredientId}`)
    if (!data.success) throw new Error(data.errorMessage || "Failed to fetch ingredient")
    return data.data
}

export function useIngredient(ingredientId: string) {
    return useQuery({
        queryKey: IngredientsKeys.detail(ingredientId),
        queryFn: () => fetchIngredient(ingredientId),
        enabled: !!ingredientId,
    })
}

async function createIngredient(payload: CreateIngredientRequest): Promise<IngredientResponse> {
    const data = await apiClient<ApiGenericResponse<IngredientResponse>>(
        "/ingredients",
        {
            method: "POST",
            body: JSON.stringify(payload),
        }
    )
    if (!data.success) throw new Error(data.errorMessage || "Failed to create ingredient")
    return data.data
}

export function useCreateIngredient() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (payload: CreateIngredientRequest) => createIngredient(payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: IngredientsKeys.lists() })
            queryClient.invalidateQueries({ queryKey: SupplierKeys.all() })
            queryClient.invalidateQueries({ queryKey: ["inventory"] })
            queryClient.invalidateQueries({ queryKey: ["pos-catalog"] })
        },
    })
}

async function updateIngredient(ingredientId: string, payload: UpdateIngredientRequest): Promise<IngredientDetailResponse> {
    const data = await apiClient<ApiGenericResponse<IngredientDetailResponse>>(`/ingredients/${ingredientId}`, {
        method: "PUT",
        body: JSON.stringify(payload),
    })
    if (!data.success) throw new Error(data.errorMessage || "Failed to update ingredient")
    return data.data
}

export function useUpdateIngredient() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ ingredientId, payload }: { ingredientId: string; payload: UpdateIngredientRequest }) =>
            updateIngredient(ingredientId, payload),
        onSuccess: (_data, variables) => {
            queryClient.invalidateQueries({ queryKey: IngredientsKeys.lists() })
            queryClient.invalidateQueries({ queryKey: IngredientsKeys.detail(variables.ingredientId) })
            queryClient.invalidateQueries({ queryKey: ["inventory"] })
            queryClient.invalidateQueries({ queryKey: ["pos-catalog"] })
        },
    })
}

async function deleteIngredient(ingredientId: string): Promise<void> {
    const data = await apiClient<ApiGenericResponse<void>>(`/ingredients/${ingredientId}`, { method: "DELETE" })
    if (!data.success) throw new Error(data.errorMessage || "Failed to delete ingredient")
}

export function useDeleteIngredient() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (ingredientId: string) => deleteIngredient(ingredientId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: IngredientsKeys.lists() })
            queryClient.invalidateQueries({ queryKey: ["inventory"] })
            queryClient.invalidateQueries({ queryKey: ["pos-catalog"] })
        },
    })
}
