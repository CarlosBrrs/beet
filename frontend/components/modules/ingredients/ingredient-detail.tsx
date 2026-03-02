"use client"

import { useIngredient } from "@/lib/hooks/use-ingredients"
import { Skeleton } from "@/components/ui/skeleton"
import { formatCurrency } from "@/lib/utils"

interface IngredientDetailProps {
    ingredientId: string
}

export function IngredientDetail({ ingredientId }: IngredientDetailProps) {
    const { data: ingredient, isLoading, isError } = useIngredient(ingredientId)

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
                        {/* Waiting on backend V4 migration for currentStock */}
                        - <span className="text-sm text-muted-foreground">{ingredient.unitAbbreviation}</span>
                    </p>
                </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Unit</h4>
                    <p>{ingredient.unitName} ({ingredient.unitAbbreviation})</p>
                </div>
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Cost per Base Unit</h4>
                    <p>{ingredient.costPerBaseUnit ? formatCurrency(ingredient.costPerBaseUnit) : "-"}</p>
                </div>
            </div>

            <div className="pt-4 border-t">
                <h4 className="text-sm font-medium text-muted-foreground mb-2">System Info</h4>
                <div className="text-xs text-muted-foreground space-y-1">
                    <p>ID: {ingredient.id}</p>
                    <p>Base Unit ID: {ingredient.baseUnitId}</p>
                </div>
            </div>
        </div>
    )
}
