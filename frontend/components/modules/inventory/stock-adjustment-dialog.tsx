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
import { CircleCheck, Loader2, Minus, Plus } from "lucide-react"
import { toast } from "sonner"
import { useForm, Controller, useWatch } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { formatPriceDisplay, formatQuantityDisplay, parsePriceInput } from "@/lib/formatters"

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
    value: z.string().min(1, { message: "Required" }),
    reason: z.enum(["ADJUSTMENT", "WASTE", "CORRECTION"], { message: "Select a reason" }),
    notes: z.string().trim().optional(),
})

type AdjustFormValues = z.infer<typeof adjustSchema>
type DeltaDirection = "ADD" | "REMOVE" | null

interface StockAdjustmentDialogProps {
    stock: InventoryStockResponse | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function StockAdjustmentDialog({ stock, open, onOpenChange }: StockAdjustmentDialogProps) {
    const { restaurantId } = useRestaurantContext()
    const [mode, setMode] = useState<"REPLACE" | "DELTA">("DELTA")
    const [deltaDirection, setDeltaDirection] = useState<DeltaDirection>(null)

    const adjustMutation = useAdjustInventoryStock(restaurantId!)

    const form = useForm<AdjustFormValues>({
        resolver: zodResolver(adjustSchema),
        mode: "onChange",
        defaultValues: { value: "", reason: undefined, notes: "" },
    })

    const watchedValue = useWatch({ control: form.control, name: "value" })

    const numericValue = useMemo(() => parsePriceInput(watchedValue || "0"), [watchedValue])
    const signedDelta = mode === "DELTA" && deltaDirection
        ? deltaDirection === "REMOVE" ? -Math.abs(numericValue) : Math.abs(numericValue)
        : null

    // Compute what the resulting stock would be
    const previewStock = useMemo(() => {
        if (!stock) return null
        if (mode === "REPLACE") return numericValue
        return signedDelta !== null ? stock.currentStock + signedDelta : null
    }, [mode, numericValue, signedDelta, stock])

    const handleSubmit = (values: AdjustFormValues) => {
        if (!stock) return
        const parsedValue = parsePriceInput(values.value)
        if (mode === "DELTA" && !deltaDirection) {
            toast.error("Select Add or Remove")
            return
        }
        if (mode === "DELTA" && parsedValue === 0) {
            toast.error("Enter a quantity greater than zero")
            return
        }
        if (mode === "REPLACE" && parsedValue < 0) {
            toast.error("Stock total cannot be negative")
            return
        }
        const adjustmentValue = mode === "DELTA" && deltaDirection === "REMOVE"
            ? -Math.abs(parsedValue)
            : mode === "DELTA" ? Math.abs(parsedValue) : parsedValue

        adjustMutation.mutate(
            {
                stockId: stock.id,
                payload: {
                    mode,
                    value: adjustmentValue,
                    reason: values.reason,
                    notes: values.notes || undefined,
                },
            },
            {
                onSuccess: () => {
                    toast.success(`Stock for ${stock.ingredientName} updated!`)
                    form.reset()
                    setMode("DELTA")
                    setDeltaDirection(null)
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
            setDeltaDirection(null)
        }
        onOpenChange(isOpen)
    }

    if (!stock) return null

    return (
        <DialogShell
            open={open}
            onOpenChange={handleClose}
            title="Adjust Stock"
            description={`${stock.ingredientName} - Current: ${stock.currentStock} ${stock.unitAbbreviation}`}
        >
            <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
                <Tabs value={mode} onValueChange={(v) => {
                    setMode(v as "REPLACE" | "DELTA")
                    setDeltaDirection(null)
                    form.setValue("value", "", { shouldValidate: true })
                }}>
                    <TabsList className="grid w-full grid-cols-2">
                        <TabsTrigger value="DELTA">Add / Remove</TabsTrigger>
                        <TabsTrigger value="REPLACE">Set Stock</TabsTrigger>
                    </TabsList>

                    <TabsContent value="DELTA" className="space-y-3 pt-2">
                        <div className="space-y-2">
                            <Label>Operation</Label>
                            <div className="grid grid-cols-2 gap-2">
                                <Button
                                    type="button"
                                    variant="outline"
                                    aria-pressed={deltaDirection === "ADD"}
                                    className={deltaDirection === "ADD"
                                        ? "border-green-600 bg-green-100 text-green-900 ring-2 ring-green-500 ring-offset-1 hover:bg-green-100 hover:text-green-900 dark:bg-green-950/60 dark:text-green-100"
                                        : "border-border bg-background text-foreground hover:border-green-400 hover:bg-green-50 hover:text-green-800 dark:hover:bg-green-950/30 dark:hover:text-green-100"}
                                    onClick={() => setDeltaDirection("ADD")}
                                >
                                    <Plus className="h-4 w-4" />
                                    Add
                                    {deltaDirection === "ADD" && <CircleCheck className="ml-auto h-4 w-4" />}
                                </Button>
                                <Button
                                    type="button"
                                    variant="outline"
                                    aria-pressed={deltaDirection === "REMOVE"}
                                    className={deltaDirection === "REMOVE"
                                        ? "border-red-600 bg-red-100 text-red-900 ring-2 ring-red-500 ring-offset-1 hover:bg-red-100 hover:text-red-900 dark:bg-red-950/60 dark:text-red-100"
                                        : "border-border bg-background text-foreground hover:border-red-400 hover:bg-red-50 hover:text-red-800 dark:hover:bg-red-950/30 dark:hover:text-red-100"}
                                    onClick={() => setDeltaDirection("REMOVE")}
                                >
                                    <Minus className="h-4 w-4" />
                                    Remove
                                    {deltaDirection === "REMOVE" && <CircleCheck className="ml-auto h-4 w-4" />}
                                </Button>
                            </div>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="delta-value">Quantity ({stock.unitAbbreviation})</Label>
                            <Input
                                id="delta-value"
                                placeholder="e.g. 5"
                                {...form.register("value")}
                                onBlur={(e) => {
                                    form.setValue("value", formatQuantityDisplay(e.target.value))
                                }}
                            />
                            <p className="text-xs text-muted-foreground">
                                Select whether this quantity enters or leaves inventory.
                            </p>
                        </div>
                    </TabsContent>

                    <TabsContent value="REPLACE" className="space-y-3 pt-2">
                        <div className="space-y-2">
                            <Label htmlFor="replace-value">New Stock ({stock.unitAbbreviation})</Label>
                            <Input
                                id="replace-value"
                                placeholder="e.g. 100"
                                {...form.register("value")}
                                onBlur={(e) => {
                                    form.setValue("value", formatPriceDisplay(e.target.value))
                                }}
                            />
                        </div>
                    </TabsContent>
                </Tabs>

                {/* Live preview of resulting stock */}
                {mode === "DELTA" ? (
                    <div className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-2 bg-muted p-3 text-center">
                        <StockAmount value={stock.currentStock} unit={stock.unitAbbreviation} />
                        <span className={`text-xl font-semibold ${deltaDirection === "ADD"
                            ? "text-green-700 dark:text-green-400"
                            : deltaDirection === "REMOVE" ? "text-red-700 dark:text-red-400" : "text-muted-foreground"}`}>
                            {deltaDirection === "ADD" ? "+" : deltaDirection === "REMOVE" ? "-" : "?"}
                        </span>
                        <StockAmount
                            value={Math.abs(numericValue)}
                            unit={stock.unitAbbreviation}
                            tone={deltaDirection === "ADD" ? "positive" : deltaDirection === "REMOVE" ? "negative" : "default"}
                        />
                        <span className="text-xl font-semibold text-muted-foreground">=</span>
                        <StockAmount value={previewStock} unit={stock.unitAbbreviation} emphasize />
                    </div>
                ) : previewStock !== null && (
                    <div className="flex items-center justify-between bg-muted p-3 text-sm">
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
                        disabled={adjustMutation.isPending
                            || !form.formState.isValid
                            || (mode === "DELTA" && (!deltaDirection || numericValue === 0))
                            || (mode === "REPLACE" && numericValue < 0)}
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

function StockAmount({
    value,
    unit,
    emphasize = false,
    tone = "default",
}: {
    value: number | null
    unit: string
    emphasize?: boolean
    tone?: "default" | "positive" | "negative"
}) {
    const toneClass = tone === "positive"
        ? "text-green-700 dark:text-green-400"
        : tone === "negative" ? "text-red-700 dark:text-red-400" : "text-foreground"
    return (
        <span className={`min-w-0 break-words text-lg font-bold ${emphasize && value !== null && value < 0 ? "text-destructive" : toneClass}`}>
            {value === null ? "--" : value.toFixed(2)} {unit}
        </span>
    )
}
