"use client"

import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Ingredient } from "@/lib/api-types"
import { useAdjustStock } from "@/lib/hooks/use-ingredients"
import { toast } from "sonner"
import { Loader2 } from "lucide-react"

// --- Validation Schemas ---
const deltaSchema = z.object({
    quantity: z.number().positive("Quantity must be positive"),
    reason: z.string().min(3, "Reason is required (min 3 chars)"),
})

const absoluteSchema = z.object({
    quantity: z.number().min(0, "Stock cannot be negative"),
    reason: z.string().min(3, "Reason is required (min 3 chars)"),
})

interface StockAdjustmentFormProps {
    ingredient: Ingredient
    onSuccess: () => void
}

export function StockAdjustmentForm({ ingredient, onSuccess }: StockAdjustmentFormProps) {
    const [mode, setMode] = useState<"DELTA" | "ABSOLUTE">("DELTA")
    const [deltaType, setDeltaType] = useState<"add" | "remove">("add")

    const { mutate: adjustStock, isPending } = useAdjustStock(ingredient.restaurantId)

    const deltaForm = useForm<z.infer<typeof deltaSchema>>({
        resolver: zodResolver(deltaSchema),
        defaultValues: { quantity: 0, reason: "" },
    })

    const absoluteForm = useForm<z.infer<typeof absoluteSchema>>({
        resolver: zodResolver(absoluteSchema),
        defaultValues: { quantity: 0, reason: "" },
    })

    const onDeltaSubmit = (values: z.infer<typeof deltaSchema>) => {
        const finalQuantity = deltaType === "add" ? values.quantity : -values.quantity

        adjustStock(
            {
                ingredientId: ingredient.id,
                adjustment: {
                    mode: "DELTA",
                    quantity: finalQuantity,
                    reason: values.reason,
                },
            },
            {
                onSuccess: () => {
                    toast.success(`Stock adjusted by ${finalQuantity} ${ingredient.unit}`)
                    onSuccess()
                    deltaForm.reset()
                },
                onError: () => toast.error("Failed to adjust stock"),
            }
        )
    }

    const onAbsoluteSubmit = (values: z.infer<typeof absoluteSchema>) => {
        adjustStock(
            {
                ingredientId: ingredient.id,
                adjustment: {
                    mode: "ABSOLUTE",
                    quantity: values.quantity,
                    reason: values.reason,
                },
            },
            {
                onSuccess: () => {
                    toast.success("Stock updated successfully")
                    onSuccess()
                    absoluteForm.reset()
                },
                onError: () => toast.error("Failed to update stock"),
            }
        )
    }

    return (
        <div className="space-y-4">
            <div className="text-sm text-muted-foreground">
                Current Stock: <span className="font-bold text-foreground">{ingredient.currentStock} {ingredient.unit}</span>
            </div>

            <Tabs defaultValue="delta" onValueChange={(v: string) => setMode(v === "delta" ? "DELTA" : "ABSOLUTE")}>
                <TabsList className="grid w-full grid-cols-2">
                    <TabsTrigger value="delta">Add / Remove</TabsTrigger>
                    <TabsTrigger value="absolute">Set Total</TabsTrigger>
                </TabsList>

                {/* --- DELTA FORM --- */}
                <TabsContent value="delta">
                    <Form {...deltaForm}>
                        <form onSubmit={deltaForm.handleSubmit(onDeltaSubmit)} className="space-y-4 pt-4">
                            <div className="flex gap-2">
                                <Button
                                    type="button"
                                    variant={deltaType === "add" ? "default" : "outline"}
                                    className={deltaType === "add" ? "bg-green-600 hover:bg-green-700" : ""}
                                    onClick={() => setDeltaType("add")}
                                >
                                    Add (+)
                                </Button>
                                <Button
                                    type="button"
                                    variant={deltaType === "remove" ? "default" : "outline"}
                                    className={deltaType === "remove" ? "bg-red-600 hover:bg-red-700" : ""}
                                    onClick={() => setDeltaType("remove")}
                                >
                                    Remove (-)
                                </Button>
                            </div>

                            <FormField
                                control={deltaForm.control}
                                name="quantity"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Quantity ({ingredient.unit})</FormLabel>
                                        <FormControl>
                                            <Input type="number" step="0.01" {...field} onChange={(e) => field.onChange(parseFloat(e.target.value))} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <FormField
                                control={deltaForm.control}
                                name="reason"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Reason</FormLabel>
                                        <FormControl>
                                            <Textarea placeholder="E.g., Delivery received, Spoilage, Theft..." {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <Button type="submit" className="w-full" disabled={isPending}>
                                {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                                Apply Adjustment
                            </Button>
                        </form>
                    </Form>
                </TabsContent>

                {/* --- ABSOLUTE FORM --- */}
                <TabsContent value="absolute">
                    <Form {...absoluteForm}>
                        <form onSubmit={absoluteForm.handleSubmit(onAbsoluteSubmit)} className="space-y-4 pt-4">
                            <FormField
                                control={absoluteForm.control}
                                name="quantity"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>New Total Stock ({ingredient.unit})</FormLabel>
                                        <FormControl>
                                            <Input type="number" step="0.01" {...field} onChange={(e) => field.onChange(parseFloat(e.target.value))} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <FormField
                                control={absoluteForm.control}
                                name="reason"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Reason</FormLabel>
                                        <FormControl>
                                            <Textarea placeholder="E.g., Inventory Count Correction..." {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <Button type="submit" className="w-full" disabled={isPending}>
                                {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                                Set New Stock
                            </Button>
                        </form>
                    </Form>
                </TabsContent>
            </Tabs>
        </div>
    )
}
