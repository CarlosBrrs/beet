"use client"

import { useState, useMemo } from "react"
import { InventoryStockResponse } from "@/lib/api-types"
import { useAdjustInventoryStock } from "@/lib/hooks/use-inventory"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { DialogShell } from "@/components/shared/dialog-shell"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { Loader2 } from "lucide-react"
import { toast } from "sonner"
import { useForm, Controller } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"

const REASON_OPTIONS = [
    {
        value: "ADJUSTMENT" as const,
        label: "Adjustment",
        description: "General stock correction due to counting, reconciliation, or transfers."
    },
    {
        value: "WASTE" as const,
        label: "Waste",
        description: "Product lost due to spoilage, expiration, spillage, or damage."
    },
    {
        value: "CORRECTION" as const,
        label: "Correction",
        description: "Fix data entry errors from a previous stock operation."
    },
]

const adjustSchema = z.object({
    value: z.number({ message: "Required" }),
    reason: z.enum(["ADJUSTMENT", "WASTE", "CORRECTION"], { message: "Select a reason" }),
    notes: z.string().trim().optional(),
})

type AdjustFormValues = z.infer<typeof adjustSchema>

interface StockAdjustmentDialogProps {
    stock: InventoryStockResponse | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function StockAdjustmentDialog({ stock, open, onOpenChange }: StockAdjustmentDialogProps) {
    const { restaurantId } = useRestaurantContext()
    const [mode, setMode] = useState<"REPLACE" | "DELTA">("DELTA")

    const adjustMutation = useAdjustInventoryStock(restaurantId!)

    const form = useForm<AdjustFormValues>({
        resolver: zodResolver(adjustSchema),
        mode: "onChange",
        defaultValues: { value: 0, reason: undefined, notes: "" },
    })

    const watchedValue = form.watch("value")

    // Compute what the resulting stock would be
    const previewStock = useMemo(() => {
        if (!stock || watchedValue === undefined || isNaN(watchedValue)) return null
        if (mode === "REPLACE") return watchedValue
        return stock.currentStock + watchedValue
    }, [stock, watchedValue, mode])

    const handleSubmit = (values: AdjustFormValues) => {
        if (!stock) return

        adjustMutation.mutate(
            {
                stockId: stock.id,
                payload: {
                    mode,
                    value: values.value,
                    reason: values.reason,
                    notes: values.notes || undefined,
                },
            },
            {
                onSuccess: () => {
                    toast.success(`Stock for ${stock.ingredientName} updated!`)
                    form.reset()
                    setMode("DELTA")
                    onOpenChange(false)
                },
                onError: (err) => {
                    toast.error(err.message || "Failed to adjust stock")
                },
            }
        )
    }

    const handleClose = (isOpen: boolean) => {
        if (!isOpen) {
            form.reset()
            setMode("DELTA")
        }
        onOpenChange(isOpen)
    }

    if (!stock) return null

    return (
        <DialogShell
            open={open}
            onOpenChange={handleClose}
            title="Adjust Stock"
            description={`${stock.ingredientName} — Current: ${stock.currentStock} ${stock.unitAbbreviation}`}
        >
            <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
                <Tabs value={mode} onValueChange={(v) => {
                    setMode(v as "REPLACE" | "DELTA")
                    form.setValue("value", 0, { shouldValidate: true })
                }}>
                    <TabsList className="grid w-full grid-cols-2">
                        <TabsTrigger value="DELTA">Add / Remove</TabsTrigger>
                        <TabsTrigger value="REPLACE">Set Stock</TabsTrigger>
                    </TabsList>

                    <TabsContent value="DELTA" className="space-y-3 pt-2">
                        <div className="space-y-2">
                            <Label htmlFor="delta-value">Delta ({stock.unitAbbreviation})</Label>
                            <Input
                                id="delta-value"
                                type="number"
                                step="0.01"
                                placeholder="e.g. -5 to remove, +10 to add"
                                {...form.register("value", { valueAsNumber: true })}
                            />
                            <p className="text-xs text-muted-foreground">
                                Use negative values to remove stock, positive to add.
                            </p>
                        </div>
                    </TabsContent>

                    <TabsContent value="REPLACE" className="space-y-3 pt-2">
                        <div className="space-y-2">
                            <Label htmlFor="replace-value">New Stock ({stock.unitAbbreviation})</Label>
                            <Input
                                id="replace-value"
                                type="number"
                                step="0.01"
                                min="0"
                                placeholder="e.g. 100"
                                {...form.register("value", { valueAsNumber: true })}
                            />
                        </div>
                    </TabsContent>
                </Tabs>

                {/* Live preview of resulting stock */}
                {previewStock !== null && (
                    <div className="flex items-center justify-between p-3 bg-muted rounded-lg text-sm">
                        <span className="text-muted-foreground">Resulting stock:</span>
                        <span className={`font-bold ${previewStock < 0 ? "text-destructive" : "text-foreground"}`}>
                            {previewStock.toFixed(2)} {stock.unitAbbreviation}
                        </span>
                    </div>
                )}

                {/* Reason */}
                <div className="space-y-2">
                    <Label>Reason</Label>
                    <Controller
                        control={form.control}
                        name="reason"
                        render={({ field }) => (
                            <Select value={field.value} onValueChange={field.onChange}>
                                <SelectTrigger>
                                    <SelectValue placeholder="Select a reason..." />
                                </SelectTrigger>
                                <SelectContent>
                                    {REASON_OPTIONS.map((opt) => (
                                        <SelectItem key={opt.value} value={opt.value}>
                                            <div>
                                                <span className="font-medium">{opt.label}</span>
                                                <p className="text-xs text-muted-foreground">{opt.description}</p>
                                            </div>
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        )}
                    />
                    {form.formState.errors.reason && (
                        <p className="text-xs text-destructive">{form.formState.errors.reason.message}</p>
                    )}
                </div>

                {/* Notes */}
                <div className="space-y-2">
                    <Label htmlFor="notes">Notes (optional)</Label>
                    <Textarea
                        id="notes"
                        placeholder="Additional details..."
                        {...form.register("notes")}
                    />
                </div>

                {/* Submit */}
                <div className="flex justify-end">
                    <Button
                        type="submit"
                        disabled={adjustMutation.isPending || !form.formState.isValid}
                    >
                        {adjustMutation.isPending ? (
                            <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Saving...</>
                        ) : (
                            "Apply Adjustment"
                        )}
                    </Button>
                </div>
            </form>
        </DialogShell>
    )
}
