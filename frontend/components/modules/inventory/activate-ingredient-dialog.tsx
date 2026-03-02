"use client"

import { useState, useMemo } from "react"
import { InventoryStockResponse } from "@/lib/api-types"
import { useAvailableIngredients, useActivateIngredient } from "@/lib/hooks/use-inventory"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { useDebounce } from "@/lib/hooks/use-debounce"
import { DialogShell } from "@/components/shared/dialog-shell"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Search, Package, Loader2 } from "lucide-react"
import { toast } from "sonner"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"

const activateSchema = z.object({
    initialStock: z.number({ message: "Required" }).min(0, "Must be >= 0"),
    minStock: z.number({ message: "Required" }).min(0, "Must be >= 0").optional(),
})

type ActivateFormValues = z.infer<typeof activateSchema>

interface ActivateIngredientDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function ActivateIngredientDialog({ open, onOpenChange }: ActivateIngredientDialogProps) {
    const { restaurantId } = useRestaurantContext()
    const [searchInput, setSearchInput] = useState("")
    const debouncedSearch = useDebounce(searchInput, 400)
    const [selectedIngredient, setSelectedIngredient] = useState<InventoryStockResponse | null>(null)

    const { data: allAvailable = [], isLoading: loadingAvailable } = useAvailableIngredients(restaurantId!)

    // Client-side search filtering
    const available = useMemo(() => {
        if (!debouncedSearch) return allAvailable
        const lower = debouncedSearch.toLowerCase()
        return allAvailable.filter(i => i.ingredientName.toLowerCase().includes(lower))
    }, [allAvailable, debouncedSearch])

    const activateMutation = useActivateIngredient(restaurantId!)

    const form = useForm<ActivateFormValues>({
        resolver: zodResolver(activateSchema),
        mode: "onChange",
        defaultValues: { initialStock: 0, minStock: 0 },
    })

    const handleActivate = (values: ActivateFormValues) => {
        if (!selectedIngredient) return

        activateMutation.mutate(
            {
                masterIngredientId: selectedIngredient.masterIngredientId,
                initialStock: values.initialStock,
                minStock: values.minStock,
            },
            {
                onSuccess: () => {
                    toast.success(`${selectedIngredient.ingredientName} activated!`)
                    setSelectedIngredient(null)
                    form.reset()
                    setSearchInput("")
                    onOpenChange(false)
                },
                onError: (err) => {
                    toast.error(err.message || "Failed to activate ingredient")
                },
            }
        )
    }

    const handleClose = (isOpen: boolean) => {
        if (!isOpen) {
            setSelectedIngredient(null)
            form.reset()
            setSearchInput("")
        }
        onOpenChange(isOpen)
    }

    return (
        <DialogShell
            open={open}
            onOpenChange={handleClose}
            title="Add from Catalog"
            description="Select an ingredient from your catalog to activate in this restaurant's inventory."
        >
            {/* TODO: Add option to remove/deactivate ingredients from catalog.
                Need to handle cases where stock > 0 or ingredient is used in products. */}
            <div className="space-y-4">
                {!selectedIngredient ? (
                    <>
                        {/* Search */}
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                            <Input
                                placeholder="Search ingredients..."
                                value={searchInput}
                                onChange={(e) => setSearchInput(e.target.value)}
                                className="pl-9"
                            />
                        </div>

                        {/* Ingredient List */}
                        <div className="max-h-[300px] overflow-y-auto border rounded-md">
                            {loadingAvailable ? (
                                <div className="flex items-center justify-center p-6">
                                    <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                                </div>
                            ) : available.length === 0 ? (
                                <div className="p-6 text-center text-sm text-muted-foreground">
                                    No ingredients available to activate.
                                </div>
                            ) : (
                                available.map((ingredient) => (
                                    <button
                                        key={ingredient.masterIngredientId}
                                        onClick={() => setSelectedIngredient(ingredient)}
                                        className="w-full flex items-center gap-3 px-4 py-3 hover:bg-muted/50 border-b last:border-b-0 text-left transition-colors"
                                    >
                                        <Package className="h-4 w-4 text-muted-foreground shrink-0" />
                                        <div className="flex-1 min-w-0">
                                            <p className="font-medium text-sm truncate">{ingredient.ingredientName}</p>
                                            <p className="text-xs text-muted-foreground">Base unit: {ingredient.unitAbbreviation}</p>
                                        </div>
                                    </button>
                                ))
                            )}
                        </div>
                    </>
                ) : (
                    /* Activation Form */
                    <form onSubmit={form.handleSubmit(handleActivate)} className="space-y-4">
                        <div className="flex items-center gap-2 p-3 bg-muted rounded-lg">
                            <Package className="h-5 w-5 text-primary" />
                            <div>
                                <p className="font-medium text-sm">{selectedIngredient.ingredientName}</p>
                                <p className="text-xs text-muted-foreground">Base unit: {selectedIngredient.unitAbbreviation}</p>
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label htmlFor="initialStock">Initial Stock ({selectedIngredient.unitAbbreviation})</Label>
                                <Input
                                    id="initialStock"
                                    type="number"
                                    step="0.01"
                                    min="0"
                                    {...form.register("initialStock", { valueAsNumber: true })}
                                />
                                {form.formState.errors.initialStock && (
                                    <p className="text-xs text-destructive">{form.formState.errors.initialStock.message}</p>
                                )}
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="minStock">Min. Stock ({selectedIngredient.unitAbbreviation})</Label>
                                <Input
                                    id="minStock"
                                    type="number"
                                    step="0.01"
                                    min="0"
                                    {...form.register("minStock", { valueAsNumber: true })}
                                />
                                {form.formState.errors.minStock && (
                                    <p className="text-xs text-destructive">{form.formState.errors.minStock.message}</p>
                                )}
                            </div>
                        </div>

                        <div className="flex gap-2 justify-end">
                            <Button type="button" variant="outline" onClick={() => setSelectedIngredient(null)}>
                                Back
                            </Button>
                            <Button
                                type="submit"
                                disabled={activateMutation.isPending || !form.formState.isValid}
                            >
                                {activateMutation.isPending ? (
                                    <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Activating...</>
                                ) : (
                                    "Activate"
                                )}
                            </Button>
                        </div>
                    </form>
                )}
            </div>
        </DialogShell>
    )
}
