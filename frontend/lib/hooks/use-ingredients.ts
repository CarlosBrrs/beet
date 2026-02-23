import { useQuery, useMutation, useQueryClient, keepPreviousData } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import {
    ApiGenericResponse,
    PageResponse,
    MockIngredient,
    CreateIngredientRequest,
    IngredientResponse,
    MockSupplier,
    IngredientListResponse,
    IngredientDetailResponse
} from "@/lib/api-types"

export interface IngredientListParams {
    page: number
    size: number
    search?: string
    sortBy?: string
    sortDesc?: boolean
    units?: string[]
}

const MOCK_INGREDIENTS: MockIngredient[] = [
    { id: "1", restaurantId: "mock", name: "Tomato", unit: "kg", cost: 1.50, currentStock: 20 },
    { id: "2", restaurantId: "mock", name: "Flour", unit: "kg", cost: 0.80, currentStock: 50 },
    { id: "3", restaurantId: "mock", name: "Mozzarella", unit: "kg", cost: 5.00, currentStock: 10 },
    { id: "4", restaurantId: "mock", name: "Basil", unit: "g", cost: 0.05, currentStock: 500 },
    { id: "5", restaurantId: "mock", name: "Olive Oil", unit: "l", cost: 8.00, currentStock: 15 },
    // Veggies
    { id: "6", restaurantId: "mock", name: "Onion", unit: "kg", cost: 1.20, currentStock: 30 },
    { id: "7", restaurantId: "mock", name: "Garlic", unit: "kg", cost: 5.00, currentStock: 5 },
    { id: "8", restaurantId: "mock", name: "Pepper", unit: "kg", cost: 2.50, currentStock: 10 },
    { id: "9", restaurantId: "mock", name: "Lettuce", unit: "unit", cost: 0.50, currentStock: 50 },
    { id: "10", restaurantId: "mock", name: "Carrot", unit: "kg", cost: 0.90, currentStock: 25 },
    { id: "11", restaurantId: "mock", name: "Cucumber", unit: "kg", cost: 1.10, currentStock: 15 },
    { id: "12", restaurantId: "mock", name: "Mushroom", unit: "kg", cost: 4.00, currentStock: 8 },
    { id: "13", restaurantId: "mock", name: "Spinach", unit: "kg", cost: 3.50, currentStock: 12 },
    { id: "14", restaurantId: "mock", name: "Broccoli", unit: "kg", cost: 2.00, currentStock: 20 },
    { id: "15", restaurantId: "mock", name: "Potato", unit: "kg", cost: 0.60, currentStock: 100 },
    // Dairy/Meat
    { id: "16", restaurantId: "mock", name: "Milk", unit: "l", cost: 1.00, currentStock: 40 },
    { id: "17", restaurantId: "mock", name: "Butter", unit: "kg", cost: 8.00, currentStock: 10 },
    { id: "18", restaurantId: "mock", name: "Cream", unit: "l", cost: 4.50, currentStock: 5 },
    { id: "19", restaurantId: "mock", name: "Cheddar Cheese", unit: "kg", cost: 7.00, currentStock: 15 },
    { id: "20", restaurantId: "mock", name: "Egg", unit: "unit", cost: 0.10, currentStock: 200 },
    { id: "21", restaurantId: "mock", name: "Beef Patty", unit: "kg", cost: 12.00, currentStock: 30 },
    { id: "22", restaurantId: "mock", name: "Chicken Breast", unit: "kg", cost: 6.00, currentStock: 40 },
    { id: "23", restaurantId: "mock", name: "Pork Chop", unit: "kg", cost: 8.00, currentStock: 25 },
    { id: "24", restaurantId: "mock", name: "Bacon", unit: "kg", cost: 10.00, currentStock: 10 },
    { id: "25", restaurantId: "mock", name: "Salmon Fillet", unit: "kg", cost: 15.00, currentStock: 5 },
    // Staples/Dry
    { id: "26", restaurantId: "mock", name: "Sugar", unit: "kg", cost: 1.00, currentStock: 50 },
    { id: "27", restaurantId: "mock", name: "Salt", unit: "kg", cost: 0.50, currentStock: 30 },
    { id: "28", restaurantId: "mock", name: "Rice", unit: "kg", cost: 1.50, currentStock: 60 },
    { id: "29", restaurantId: "mock", name: "Pasta Penne", unit: "kg", cost: 2.00, currentStock: 40 },
    { id: "30", restaurantId: "mock", name: "Sunflower Oil", unit: "l", cost: 3.00, currentStock: 20 },
    { id: "31", restaurantId: "mock", name: "Balsamic Vinegar", unit: "l", cost: 5.00, currentStock: 10 },
    { id: "32", restaurantId: "mock", name: "Yeast", unit: "kg", cost: 5.00, currentStock: 2 },
    { id: "33", restaurantId: "mock", name: "Coffee Beans", unit: "kg", cost: 12.00, currentStock: 10 },
    { id: "34", restaurantId: "mock", name: "Black Tea", unit: "kg", cost: 20.00, currentStock: 5 },
    { id: "35", restaurantId: "mock", name: "Dark Chocolate", unit: "kg", cost: 9.00, currentStock: 8 },
]

// ── Mock Suppliers (simulates existing suppliers for the form) ──
// TODO: [SCALABILITY] Evaluate supplier retrieval strategy before going to production.
// Currently loads ALL suppliers in-memory. If the supplier list grows significantly,
// consider:
//   1. Server-side pagination (cursor or offset) with a search/filter endpoint
//   2. Debounced search-as-you-type with React Query + keepPreviousData
//   3. Virtualised Select dropdown (e.g. react-select + react-window)
//   4. Caching strategy: how long to cache, stale-while-revalidate, etc.
// This is fine for < 200 suppliers, but will degrade beyond that.

const MOCK_SUPPLIERS: MockSupplier[] = [
    { id: "s1", name: "Molinos del Sur", documentTypeId: "dt-nit", documentNumber: "900-123-456" },
    { id: "s2", name: "Distribuidora La Central", documentTypeId: "dt-nit", documentNumber: "800-456-789" },
    { id: "s3", name: "Lácteos El Prado", documentTypeId: "dt-cc", documentNumber: "12345678" },
    { id: "s4", name: "Carnes Premium S.A.", documentTypeId: "dt-nit", documentNumber: "901-222-333" },
    { id: "s5", name: "Frutas y Verduras Express", documentTypeId: "dt-cc", documentNumber: "98765432" },
]

export function useMockSuppliers() {
    return MOCK_SUPPLIERS
}

// ── Query Keys ──

export const IngredientsKeys = {
    all: () => ["ingredients"] as const,
    lists: () => [...IngredientsKeys.all(), "list"] as const,
    list: (params: IngredientListParams) => [...IngredientsKeys.lists(), params] as const,
    details: () => [...IngredientsKeys.all(), "detail"] as const,
    detail: (id: string) => [...IngredientsKeys.details(), id] as const,
}

// ── List (Server-Side Pagination) ──

async function fetchIngredients(params: IngredientListParams): Promise<PageResponse<IngredientListResponse>> {
    const searchParams = new URLSearchParams()
    searchParams.append("page", params.page.toString())
    searchParams.append("size", params.size.toString())
    if (params.search) searchParams.append("search", params.search)
    if (params.sortBy) searchParams.append("sortBy", params.sortBy)
    if (params.sortDesc) searchParams.append("sortDesc", params.sortDesc.toString())
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

// ── Detail (mock) ──
// TODO: Connect this to real backend endpoint /ingredients/{id} when ready
async function fetchIngredient(ingredientId: string): Promise<IngredientDetailResponse> {
    await new Promise(resolve => setTimeout(resolve, 500))
    const item = MOCK_INGREDIENTS.find(i => i.id === ingredientId)
    if (!item) throw new Error("Ingredient not found")

    // Convert MockIngredient to IngredientDetailResponse
    return {
        id: item.id,
        name: item.name,
        baseUnitId: "mock-unit-id",
        unitName: item.unit === 'kg' ? 'Kilogram' : item.unit,
        unitAbbreviation: item.unit,
        costPerBaseUnit: item.cost,
        activeSupplier: null
    }
}

export function useIngredient(ingredientId: string) {
    return useQuery({
        queryKey: IngredientsKeys.detail(ingredientId),
        queryFn: () => fetchIngredient(ingredientId),
        enabled: !!ingredientId,
    })
}

// ── Create (REAL — calls backend POST /ingredients) ──

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
        },
    })
}

// ── Stock Adjustment (mock) ──

export interface StockAdjustmentRequest {
    mode: "DELTA" | "ABSOLUTE"
    quantity: number
    reason: string
}

async function adjustStock(ingredientId: string, adjustment: StockAdjustmentRequest) {
    // 🚧 STILL MOCKED until backend implements it 🚧
    await new Promise(resolve => setTimeout(resolve, 500))
    const item = MOCK_INGREDIENTS.find(i => i.id === ingredientId)
    if (!item) throw new Error("Ingredient not found")

    if (adjustment.mode === "DELTA") {
        item.currentStock += adjustment.quantity
    } else {
        item.currentStock = adjustment.quantity
    }
    return item
}

export function useAdjustStock() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ ingredientId, adjustment }: { ingredientId: string; adjustment: StockAdjustmentRequest }) =>
            adjustStock(ingredientId, adjustment),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: IngredientsKeys.lists() })
        },
    })
}

// ── Delete (mock) ──

async function deleteIngredient(ingredientId: string) {
    // 🚧 STILL MOCKED until backend implements it 🚧
    await new Promise(resolve => setTimeout(resolve, 500))
    const index = MOCK_INGREDIENTS.findIndex(i => i.id === ingredientId)
    if (index !== -1) {
        MOCK_INGREDIENTS.splice(index, 1)
    }
    return true
}

export function useDeleteIngredient() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (ingredientId: string) => deleteIngredient(ingredientId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: IngredientsKeys.lists() })
        },
    })
}
