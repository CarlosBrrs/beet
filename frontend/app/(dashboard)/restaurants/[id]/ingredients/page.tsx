"use client"

import { useState } from "react"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { useIngredients, useCreateIngredient } from "@/lib/hooks/use-ingredients"
import { Ingredient } from "@/lib/api-types"
import { IngredientList } from "@/components/modules/ingredients/ingredient-list"
import { SheetShell } from "@/components/shared/sheet-shell"
import { IngredientForm } from "@/components/modules/ingredients/ingredient-form"
import { IngredientDetail } from "@/components/modules/ingredients/ingredient-detail"
import { Button } from "@/components/ui/button"
import { Plus } from "lucide-react"
import { Can } from "@/components/shared/can"
import { PermissionModule, PermissionAction } from "@/lib/permissions"
import { DialogShell } from "@/components/shared/dialog-shell"
import { StockAdjustmentForm } from "@/components/modules/ingredients/stock-adjustment-form"
import { useDeleteIngredient } from "@/lib/hooks/use-ingredients"
import { toast } from "sonner"

export default function IngredientsPage() {
    const { restaurantId } = useRestaurantContext()
    const { data: ingredients, isLoading, isError } = useIngredients(restaurantId || "")
    const createMutation = useCreateIngredient(restaurantId || "")

    const { mutate: deleteIngredient } = useDeleteIngredient(restaurantId || "")

    const [selectedIngredient, setSelectedIngredient] = useState<Ingredient | null>(null) // For View/Edit
    const [adjustmentIngredient, setAdjustmentIngredient] = useState<Ingredient | null>(null) // For Stock
    const [isSheetOpen, setIsSheetOpen] = useState(false)
    const [isReadOnly, setIsReadOnly] = useState(false)

    const handleCreate = (values: any) => {
        createMutation.mutate(values, {
            onSuccess: () => {
                setIsSheetOpen(false)
            }
        })
    }

    // Handlers for Sheet (View/Edit)
    const openCreate = () => {
        setSelectedIngredient(null)
        setIsReadOnly(false)
        setIsSheetOpen(true)
    }

    const openView = (ingredient: Ingredient) => {
        setSelectedIngredient(ingredient)
        setIsReadOnly(true)
        setIsSheetOpen(true)
    }

    const openEdit = (ingredient: Ingredient) => {
        setSelectedIngredient(ingredient)
        setIsReadOnly(false)
        setIsSheetOpen(true)
    }

    // Handlers for Stock/Delete
    const openAdjust = (ingredient: Ingredient) => {
        setAdjustmentIngredient(ingredient)
    }

    const handleDelete = (ingredient: Ingredient) => {
        if (confirm(`Are you sure you want to delete ${ingredient.name}? This action cannot be undone.`)) {
            deleteIngredient(ingredient.id, {
                onSuccess: () => toast.success("Ingredient deleted"),
                onError: () => toast.error("Failed to delete ingredient"),
            })
        }
    }

    if (isError) return <div>Error loading ingredients</div>

    return (
        <div className="space-y-4 pb-6">
            <div className="flex justify-between items-center">
                <div>
                    <h2 className="text-2xl font-bold tracking-tight">Ingredients</h2>
                    <p className="text-muted-foreground">Manage your raw inventory items.</p>
                </div>

                <Can I={PermissionAction.CREATE} a={PermissionModule.INVENTORY}>
                    <Button onClick={openCreate}>
                        <Plus className="mr-2 h-4 w-4" /> Add Ingredient
                    </Button>
                </Can>
            </div>

            <IngredientList
                ingredients={ingredients || []}
                isLoading={isLoading}
                onView={openView}
                onEdit={openEdit}
                onAdjust={openAdjust}
                onDelete={handleDelete}
            />

            {/* View/Edit Sheet */}
            <SheetShell
                open={isSheetOpen}
                onOpenChange={setIsSheetOpen}
                title={selectedIngredient ? (isReadOnly ? "View Ingredient" : "Edit Ingredient") : "New Ingredient"}
                description={selectedIngredient ? (isReadOnly ? "Details of the ingredient" : "Update ingredient details") : "Add a new ingredient to your inventory"}
            >
                {selectedIngredient && isReadOnly ? (
                    <IngredientDetail
                        restaurantId={restaurantId || ""}
                        ingredientId={selectedIngredient.id}
                    />
                ) : (
                    <IngredientForm
                        defaultValues={selectedIngredient || undefined}
                        onSubmit={handleCreate}
                        isSubmitting={createMutation.isPending}
                    />
                )}
            </SheetShell>

            {/* Stock Adjustment Dialog */}
            {adjustmentIngredient && (
                <DialogShell
                    open={!!adjustmentIngredient}
                    onOpenChange={() => setAdjustmentIngredient(null)}
                    title={`Adjust Stock: ${adjustmentIngredient.name}`}
                >
                    <StockAdjustmentForm
                        ingredient={adjustmentIngredient}
                        onSuccess={() => setAdjustmentIngredient(null)}
                    />
                </DialogShell>
            )}
        </div>
    )
}
