"use client"

import { useIngredient } from "@/lib/hooks/use-ingredients"
import { Skeleton } from "@/components/ui/skeleton"
import { formatCurrency } from "@/lib/utils"

interface IngredientDetailProps {
    restaurantId: string
    ingredientId: string
}

export function IngredientDetail({ restaurantId, ingredientId }: IngredientDetailProps) {
    const { data: ingredient, isLoading, isError } = useIngredient(restaurantId, ingredientId)

    if (isLoading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-4 w-[250px]" />
                <Skeleton className="h-4 w-[200px]" />
                <Skeleton className="h-4 w-[150px]" />
            </div>
        )
    }

    if (isError || !ingredient) {
        return <div className="text-destructive">Failed to load ingredient details.</div>
    }

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-2 gap-4">
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Name</h4>
                    <p className="text-lg font-semibold">{ingredient.name}</p>
                </div>
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Current Stock</h4>
                    <p className="text-lg">
                        {ingredient.currentStock} <span className="text-sm text-muted-foreground">{ingredient.unit}</span>
                    </p>
                </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Unit</h4>
                    <p>{ingredient.unit}</p>
                </div>
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Cost per Unit</h4>
                    <p>{formatCurrency(ingredient.cost)}</p>
                </div>
            </div>

            <div className="pt-4 border-t">
                <h4 className="text-sm font-medium text-muted-foreground mb-2">System Info</h4>
                <div className="text-xs text-muted-foreground space-y-1">
                    <p>ID: {ingredient.id}</p>
                    <p>Restaurant ID: {ingredient.restaurantId}</p>
                </div>
            </div>
        </div>
    )
}
