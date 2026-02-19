import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import { ApiGenericResponse, Ingredient, CreateIngredientRequest, UpdateIngredientRequest } from "@/lib/api-types"

const USE_MOCK_DATA = true // Toggle this to false when backend is ready

const MOCK_INGREDIENTS: Ingredient[] = [
    { id: "1", restaurantId: "mock", name: "Tomato", unit: "kg", cost: 1.50, currentStock: 20 },
    { id: "2", restaurantId: "mock", name: "Flour", unit: "kg", cost: 0.80, currentStock: 50 },
    { id: "3", restaurantId: "mock", name: "Mozzarella", unit: "kg", cost: 5.00, currentStock: 10 },
    { id: "4", restaurantId: "mock", name: "Basil", unit: "g", cost: 0.05, currentStock: 500 },
    { id: "5", restaurantId: "mock", name: "Olive Oil", unit: "l", cost: 8.00, currentStock: 15 },
    // --- Added 30 More Ingredients ---
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

// Query Keys
export const IngredientsKeys = {
    all: (restaurantId: string) => ["ingredients", restaurantId] as const,
    lists: (restaurantId: string) => [...IngredientsKeys.all(restaurantId), "list"] as const,
    details: (restaurantId: string) => [...IngredientsKeys.all(restaurantId), "detail"] as const,
    detail: (restaurantId: string, id: string) => [...IngredientsKeys.details(restaurantId), id] as const,
}

// Fetcher Function
async function fetchIngredients(restaurantId: string): Promise<Ingredient[]> {
    if (USE_MOCK_DATA) {
        // Simulate network delay
        await new Promise(resolve => setTimeout(resolve, 500))
        return MOCK_INGREDIENTS
    }

    const data = await apiClient<ApiGenericResponse<Ingredient[]>>(
        `/restaurants/${restaurantId}/ingredients`
    )
    if (!data.success) throw new Error(data.errorMessage || "Failed to fetch ingredients")
    return data.data
}

// Hook
export function useIngredients(restaurantId: string) {
    return useQuery({
        queryKey: IngredientsKeys.lists(restaurantId),
        queryFn: () => fetchIngredients(restaurantId),
        enabled: !!restaurantId,
    })
}

async function fetchIngredient(restaurantId: string, ingredientId: string): Promise<Ingredient> {
    if (USE_MOCK_DATA) {
        await new Promise(resolve => setTimeout(resolve, 500))
        const item = MOCK_INGREDIENTS.find(i => i.id === ingredientId)
        if (!item) throw new Error("Ingredient not found")
        return item
    }

    const data = await apiClient<ApiGenericResponse<Ingredient>>(
        `/restaurants/${restaurantId}/ingredients/${ingredientId}`
    )
    if (!data.success) throw new Error(data.errorMessage || "Failed to fetch ingredient")
    return data.data
}

export function useIngredient(restaurantId: string, ingredientId: string) {
    return useQuery({
        queryKey: IngredientsKeys.detail(restaurantId, ingredientId),
        queryFn: () => fetchIngredient(restaurantId, ingredientId),
        enabled: !!restaurantId && !!ingredientId,
    })
}

// Mutations
async function createIngredient(restaurantId: string, newItem: CreateIngredientRequest) {
    if (USE_MOCK_DATA) {
        await new Promise(resolve => setTimeout(resolve, 500))
        const mockItem: Ingredient = {
            id: Math.random().toString(36).substr(2, 9),
            restaurantId,
            currentStock: 0,
            ...newItem
        }
        MOCK_INGREDIENTS.push(mockItem) // Update local mock (dirty but works for demo)
        return mockItem
    }

    const data = await apiClient<ApiGenericResponse<Ingredient>>(
        `/restaurants/${restaurantId}/ingredients`,
        {
            method: "POST",
            body: JSON.stringify(newItem),
        }
    )
    return data.data
}

export function useCreateIngredient(restaurantId: string) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (newItem: CreateIngredientRequest) => createIngredient(restaurantId, newItem),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: IngredientsKeys.lists(restaurantId) })
        },
    })
}

export interface StockAdjustmentRequest {
    mode: "DELTA" | "ABSOLUTE"
    quantity: number
    reason: string
}

async function adjustStock(restaurantId: string, ingredientId: string, adjustment: StockAdjustmentRequest) {
    if (USE_MOCK_DATA) {
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

    // Backend implementation pending
    // const data = await apiClient(...)
    // return data.data
}

export function useAdjustStock(restaurantId: string) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ ingredientId, adjustment }: { ingredientId: string; adjustment: StockAdjustmentRequest }) =>
            adjustStock(restaurantId, ingredientId, adjustment),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: IngredientsKeys.lists(restaurantId) })
        },
    })
}

async function deleteIngredient(restaurantId: string, ingredientId: string) {
    if (USE_MOCK_DATA) {
        await new Promise(resolve => setTimeout(resolve, 500))
        const index = MOCK_INGREDIENTS.findIndex(i => i.id === ingredientId)
        if (index !== -1) {
            MOCK_INGREDIENTS.splice(index, 1)
        }
        return true
    }
    // Backend implementation pending
}

export function useDeleteIngredient(restaurantId: string) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (ingredientId: string) => deleteIngredient(restaurantId, ingredientId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: IngredientsKeys.lists(restaurantId) })
        },
    })
}
