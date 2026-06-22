"use client"

import { useState } from "react"
import { useCreateIngredient, useDeleteIngredient, useIngredient, useUpdateIngredient } from "@/lib/hooks/use-ingredients"
import { IngredientListResponse, CreateIngredientRequest } from "@/lib/api-types"
import { IngredientList } from "@/components/modules/ingredients/ingredient-list"
import { SheetShell } from "@/components/shared/sheet-shell"
import { IngredientForm } from "@/components/modules/ingredients/ingredient-form"
import { IngredientEditForm } from "@/components/modules/ingredients/ingredient-edit-form"
import { IngredientDetail } from "@/components/modules/ingredients/ingredient-detail"
import { Button } from "@/components/ui/button"
import { Plus } from "lucide-react"
import { DialogShell } from "@/components/shared/dialog-shell"
import { toast } from "sonner"

export default function IngredientsPage() {
    const createMutation = useCreateIngredient()
    const updateMutation = useUpdateIngredient()
    const deleteMutation = useDeleteIngredient()

    const [selectedIngredient, setSelectedIngredient] = useState<IngredientListResponse | null>(null)
    const [deleteTarget, setDeleteTarget] = useState<IngredientListResponse | null>(null)
    const [isSheetOpen, setIsSheetOpen] = useState(false)
    const [isReadOnly, setIsReadOnly] = useState(false)

    const { data: selectedIngredientDetail, isLoading: selectedIngredientLoading } = useIngredient(
        selectedIngredient && !isReadOnly ? selectedIngredient.id : ""
    )

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

    const handleDelete = (ingredient: IngredientListResponse) => {
        setDeleteTarget(ingredient)
    }

    const confirmDelete = () => {
        if (!deleteTarget) return
        deleteMutation.mutate(deleteTarget.id, {
            onSuccess: () => {
                toast.success("Ingredient deleted")
                setDeleteTarget(null)
            },
            onError: (error) => toast.error(error.message || "Failed to delete ingredient"),
        })
    }

    return (
        <div className="space-y-4 pb-8">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h2 className="text-2xl font-bold tracking-tight">Ingredients</h2>
                    <p className="text-muted-foreground">Manage your shared raw inventory catalog.</p>
                </div>

                <Button onClick={openCreate}>
                    <Plus className="mr-2 h-4 w-4" /> Add Ingredient
                </Button>
            </div>

            <IngredientList
                onView={handleView}
                onEdit={handleEdit}
                onDelete={handleDelete}
            />

            <SheetShell
                open={isSheetOpen}
                onOpenChange={setIsSheetOpen}
                title={selectedIngredient ? (isReadOnly ? "View Ingredient" : "Edit Ingredient") : "New Ingredient"}
                description={selectedIngredient ? (isReadOnly ? "Details of the ingredient" : "Update basic ingredient metadata") : "Add a new ingredient to your inventory"}
                size="lg"
            >
                {selectedIngredient && isReadOnly ? (
                    <IngredientDetail ingredientId={selectedIngredient.id} />
                ) : selectedIngredient ? (
                    selectedIngredientLoading || !selectedIngredientDetail ? (
                        <div className="py-8 text-sm text-muted-foreground">Loading ingredient...</div>
                    ) : (
                        <IngredientEditForm
                            ingredient={selectedIngredientDetail}
                            isSubmitting={updateMutation.isPending}
                            onSubmit={(values) => {
                                updateMutation.mutate({ ingredientId: selectedIngredient.id, payload: values }, {
                                    onSuccess: () => {
                                        toast.success("Ingredient updated")
                                        setIsSheetOpen(false)
                                    },
                                    onError: (error) => toast.error(error.message || "Failed to update ingredient"),
                                })
                            }}
                        />
                    )
                ) : (
                    <IngredientForm
                        onSubmit={handleCreate}
                        isSubmitting={createMutation.isPending}
                    />
                )}
            </SheetShell>

            {deleteTarget && (
                <DialogShell
                    open={!!deleteTarget}
                    onOpenChange={(open) => !open && setDeleteTarget(null)}
                    title="Delete ingredient"
                    description="This performs a soft delete only if the ingredient has no stock, recipes, orders or inventory history."
                >
                    <div className="space-y-4">
                        <p className="text-sm">
                            Delete <span className="font-medium">{deleteTarget.name}</span>?
                        </p>
                        <div className="flex justify-end gap-2">
                            <Button variant="outline" onClick={() => setDeleteTarget(null)}>Cancel</Button>
                            <Button variant="destructive" onClick={confirmDelete} disabled={deleteMutation.isPending}>
                                Delete
                            </Button>
                        </div>
                    </div>
                </DialogShell>
            )}
        </div>
    )
}
