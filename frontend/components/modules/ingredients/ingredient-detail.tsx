"use client"

import { useIngredient } from "@/lib/hooks/use-ingredients"
import { Skeleton } from "@/components/ui/skeleton"
import { Badge } from "@/components/ui/badge"
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
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Name</h4>
                    <p className="text-lg font-semibold">{ingredient.name}</p>
                </div>
                <Badge variant={ingredient.costComplete ? "default" : "secondary"}>
                    {ingredient.costComplete ? "Cost complete" : "Cost incomplete"}
                </Badge>
            </div>

            <div className="grid grid-cols-2 gap-4">
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Current Stock</h4>
                    <p className="text-lg">
                        {ingredient.currentStock ?? 0} <span className="text-sm text-muted-foreground">{ingredient.unitAbbreviation}</span>
                    </p>
                </div>
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Unit</h4>
                    <p>{ingredient.unitName} ({ingredient.unitAbbreviation})</p>
                </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Cost per Base Unit</h4>
                    <p>{ingredient.costPerBaseUnit ? formatCurrency(ingredient.costPerBaseUnit) : "-"}</p>
                </div>
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Active Supplier</h4>
                    <p>{ingredient.activeSupplier?.supplierName ?? "-"}</p>
                </div>
            </div>

            {ingredient.activeSupplier && (
                <div className="rounded-md border p-4 text-sm">
                    <h4 className="font-medium mb-2">Active supplier item</h4>
                    <div className="grid grid-cols-2 gap-3 text-muted-foreground">
                        <p>Brand: <span className="text-foreground">{ingredient.activeSupplier.brandName || "-"}</span></p>
                        <p>Package: <span className="text-foreground">{ingredient.activeSupplier.purchaseUnitName}</span></p>
                        <p>Conversion: <span className="text-foreground">{ingredient.activeSupplier.conversionFactor}</span></p>
                        <p>Last cost: <span className="text-foreground">{formatCurrency(ingredient.activeSupplier.lastCostBase)}</span></p>
                    </div>
                </div>
            )}

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
