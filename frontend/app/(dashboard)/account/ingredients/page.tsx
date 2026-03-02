"use client"

import { useState } from "react"
import { useCreateIngredient } from "@/lib/hooks/use-ingredients"
import { IngredientListResponse, CreateIngredientRequest } from "@/lib/api-types"
import { IngredientList } from "@/components/modules/ingredients/ingredient-list"
import { SheetShell } from "@/components/shared/sheet-shell"
import { IngredientForm } from "@/components/modules/ingredients/ingredient-form"
import { IngredientDetail } from "@/components/modules/ingredients/ingredient-detail"
import { Button } from "@/components/ui/button"
import { Plus } from "lucide-react"
import { DialogShell } from "@/components/shared/dialog-shell"
import { StockAdjustmentForm } from "@/components/modules/ingredients/stock-adjustment-form"
import { useDeleteIngredient } from "@/lib/hooks/use-ingredients"
import { toast } from "sonner"

// TODO: [SCALABILITY] The supplier dropdown in IngredientForm currently loads ALL suppliers
// via useMockSuppliers(). Before scaling, evaluate:
//   - Paginated / search-based API for suppliers (GET /suppliers?q=…&limit=20)
//   - Async Select component with debounced search
//   - Impact on UX when supplier count > 200
// See also: use-ingredients.ts → useMockSuppliers()

export default function IngredientsPage() {
    const createMutation = useCreateIngredient()

    const { mutate: deleteIngredient } = useDeleteIngredient()

    const [selectedIngredient, setSelectedIngredient] = useState<IngredientListResponse | null>(null)
    const [adjustmentIngredient, setAdjustmentIngredient] = useState<IngredientListResponse | null>(null)
    const [isSheetOpen, setIsSheetOpen] = useState(false)
    const [isReadOnly, setIsReadOnly] = useState(false)

    const handleCreate = (values: CreateIngredientRequest) => {
        createMutation.mutate(values, {
            onSuccess: () => {
                toast.success("Ingredient created successfully")
                setIsSheetOpen(false)
            },
            onError: (error) => {
                toast.error(error.message || "Failed to create ingredient")
            },
        })
    }

    // Handlers for Sheet (View/Edit)
    const openCreate = () => {
        setSelectedIngredient(null)
        setIsReadOnly(false)
        setIsSheetOpen(true)
    }

    const handleView = (ingredient: IngredientListResponse) => {
        setSelectedIngredient(ingredient)
        setIsReadOnly(true)
        setIsSheetOpen(true)
    }

    const handleEdit = (ingredient: IngredientListResponse) => {
        setSelectedIngredient(ingredient)
        setIsReadOnly(false)
        setIsSheetOpen(true)
    }

    // Handlers for Stock Adjustment Modal
    const handleAdjust = (ingredient: IngredientListResponse) => {
        setAdjustmentIngredient(ingredient)
    }

    const handleDelete = (ingredient: IngredientListResponse) => {
        if (confirm(`Are you sure you want to delete ${ingredient.name}? This action cannot be undone.`)) {
            deleteIngredient(ingredient.id, {
                onSuccess: () => toast.success("Ingredient deleted"),
                onError: () => toast.error("Failed to delete ingredient"),
            })
        }
    }

    return (
        <div className="space-y-4 pb-6">
            <div className="flex justify-between items-center">
                <div>
                    <h2 className="text-2xl font-bold tracking-tight">Ingredients</h2>
                    <p className="text-muted-foreground">Manage your raw inventory items.</p>
                </div>

                <Button onClick={openCreate}>
                    <Plus className="mr-2 h-4 w-4" /> Add Ingredient
                </Button>
            </div>

            <IngredientList
                onView={handleView}
                onEdit={handleEdit}
                onAdjust={handleAdjust}
                onDelete={handleDelete}
            />

            {/* Create / View / Edit Sheet */}
            <SheetShell
                open={isSheetOpen}
                onOpenChange={setIsSheetOpen}
                title={selectedIngredient ? (isReadOnly ? "View Ingredient" : "Edit Ingredient") : "New Ingredient"}
                description={selectedIngredient ? (isReadOnly ? "Details of the ingredient" : "Update ingredient details") : "Add a new ingredient to your inventory"}
                size="lg"
            >
                {selectedIngredient && isReadOnly ? (
                    <IngredientDetail
                        ingredientId={selectedIngredient.id}
                    />
                ) : selectedIngredient ? (
                    /* Edit mode — still mocked, provides a placeholder */
                    <div className="text-sm text-muted-foreground py-4">
                        Edit mode is not yet connected to the backend.
                    </div>
                ) : (
                    <IngredientForm
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
