"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2 } from "lucide-react"
import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { DialogShell } from "@/components/shared/dialog-shell"
import { Button } from "@/components/ui/button"
import {
    Form,
    FormControl,
    FormDescription,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { OrderItemDetailResponse, OrderItemInventoryDisposition } from "@/lib/api-types"
import { useCancelOrderItem } from "@/lib/hooks/use-orders"

const dispositions = ["NO_RESTOCK", "RESTOCK", "WASTE"] as const

interface CancelOrderItemDialogProps {
    orderId: string
    item: OrderItemDetailResponse
    requiresDisposition: boolean
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function CancelOrderItemDialog({
    orderId,
    item,
    requiresDisposition,
    open,
    onOpenChange,
}: CancelOrderItemDialogProps) {
    const schema = z.object({
        quantity: z.number().int().min(1).max(item.activeQuantity),
        reason: z.string().trim().min(3, "Indica el motivo de la cancelacion."),
        inventoryDisposition: z.enum(dispositions).optional(),
    }).superRefine((values, context) => {
        if (requiresDisposition && !values.inventoryDisposition) {
            context.addIssue({
                code: "custom",
                path: ["inventoryDisposition"],
                message: "Selecciona que sucede con el inventario consumido.",
            })
        }
    })
    type Values = z.infer<typeof schema>
    const cancelItem = useCancelOrderItem(orderId)
    const form = useForm<Values>({
        resolver: zodResolver(schema),
        defaultValues: {
            quantity: 1,
            reason: "",
            inventoryDisposition: undefined,
        },
    })

    useEffect(() => {
        if (open) {
            form.reset({ quantity: 1, reason: "", inventoryDisposition: undefined })
        }
    }, [form, open])

    const handleSubmit = (values: Values) => {
        cancelItem.mutate(
            {
                orderItemId: item.id,
                request: {
                    quantity: values.quantity,
                    reason: values.reason,
                    inventoryDisposition: values.inventoryDisposition as OrderItemInventoryDisposition | undefined,
                },
            },
            { onSuccess: () => onOpenChange(false) }
        )
    }

    return (
        <DialogShell
            open={open}
            onOpenChange={onOpenChange}
            title={`Cancelar ${item.itemNameSnapshot}`}
            description={`Cantidad activa: ${item.activeQuantity}. La cantidad original se conserva para auditoria.`}
        >
            <Form {...form}>
                <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
                    <FormField
                        control={form.control}
                        name="quantity"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Unidades a cancelar</FormLabel>
                                <FormControl>
                                    <Input
                                        type="number"
                                        min={1}
                                        max={item.activeQuantity}
                                        value={field.value}
                                        onChange={(event) => field.onChange(Number(event.target.value))}
                                    />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    {requiresDisposition && (
                        <FormField
                            control={form.control}
                            name="inventoryDisposition"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Inventario consumido</FormLabel>
                                    <Select value={field.value} onValueChange={field.onChange}>
                                        <FormControl>
                                            <SelectTrigger><SelectValue placeholder="Selecciona una decision" /></SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            <SelectItem value="RESTOCK">Reponer al inventario</SelectItem>
                                            <SelectItem value="NO_RESTOCK">No reponer</SelectItem>
                                            <SelectItem value="WASTE">Registrar como desperdicio</SelectItem>
                                        </SelectContent>
                                    </Select>
                                    <FormDescription>
                                        Reponer crea movimientos positivos por cada ingrediente. Las otras opciones no modifican el stock nuevamente.
                                    </FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    )}

                    <FormField
                        control={form.control}
                        name="reason"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Motivo</FormLabel>
                                <FormControl>
                                    <Textarea {...field} className="resize-none" placeholder="Motivo operativo de la cancelacion" />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <div className="flex justify-end">
                        <Button type="submit" variant="destructive" disabled={cancelItem.isPending}>
                            {cancelItem.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                            Cancelar unidades
                        </Button>
                    </div>
                </form>
            </Form>
        </DialogShell>
    )
}
