"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2 } from "lucide-react"
import { useEffect, useMemo } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { DialogShell } from "@/components/shared/dialog-shell"
import { Button } from "@/components/ui/button"
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { OrderDetailResponse, OrderItemDetailResponse, OrderItemInventoryDisposition } from "@/lib/api-types"
import { useCancelOrder } from "@/lib/hooks/use-orders"

const dispositions = ["NO_RESTOCK", "RESTOCK", "WASTE"] as const

const schema = z.object({
    reason: z.string().trim().min(3, "Indica el motivo de la cancelacion."),
    decisions: z.record(z.string(), z.enum(dispositions).or(z.literal(""))),
})

type Values = z.infer<typeof schema>

interface CancelOrderDialogProps {
    order: OrderDetailResponse
    open: boolean
    onOpenChange: (open: boolean) => void
    requiresDisposition: (item: OrderItemDetailResponse) => boolean
}

export function CancelOrderDialog({ order, open, onOpenChange, requiresDisposition }: CancelOrderDialogProps) {
    const cancelOrder = useCancelOrder()
    const preparedItems = useMemo(
        () => order.items.filter((item) => item.activeQuantity > 0 && requiresDisposition(item)),
        [order.items, requiresDisposition]
    )
    const form = useForm<Values>({
        resolver: zodResolver(schema),
        defaultValues: { reason: "", decisions: {} },
    })

    useEffect(() => {
        if (open) {
            const decisions: Record<string, "" | "NO_RESTOCK" | "RESTOCK" | "WASTE"> =
                Object.fromEntries(preparedItems.map((item) => [item.id, ""]))
            form.reset({ reason: "", decisions })
        }
    }, [form, open, preparedItems])

    const submit = (values: Values) => {
        const missing = preparedItems.find((item) => !values.decisions[item.id])
        if (missing) {
            form.setError(`decisions.${missing.id}` as "decisions", {
                message: "Selecciona una decision para cada linea preparada.",
            })
            return
        }
        cancelOrder.mutate(
            {
                orderId: order.id,
                request: {
                    reason: values.reason,
                    lineDecisions: preparedItems.map((item) => ({
                        orderItemId: item.id,
                        inventoryDisposition: values.decisions[item.id] as OrderItemInventoryDisposition,
                    })),
                },
            },
            { onSuccess: () => onOpenChange(false) }
        )
    }

    return (
        <DialogShell
            open={open}
            onOpenChange={onOpenChange}
            title={`Cancelar orden ${order.displayCode}`}
            description="La orden quedara cancelada y las unidades activas quedaran auditadas como canceladas."
            size="lg"
        >
            <Form {...form}>
                <form onSubmit={form.handleSubmit(submit)} className="space-y-4">
                    {preparedItems.length > 0 && (
                        <div className="space-y-3 border bg-muted/20 p-3">
                            <p className="text-xs font-medium">Lineas con inventario ya consumido</p>
                            {preparedItems.map((item) => (
                                <FormField
                                    key={item.id}
                                    control={form.control}
                                    name={`decisions.${item.id}`}
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>{item.itemNameSnapshot} · x{item.activeQuantity}</FormLabel>
                                            <Select value={field.value} onValueChange={field.onChange}>
                                                <FormControl>
                                                    <SelectTrigger><SelectValue placeholder="Decision de inventario" /></SelectTrigger>
                                                </FormControl>
                                                <SelectContent>
                                                    <SelectItem value="RESTOCK">Reponer al inventario</SelectItem>
                                                    <SelectItem value="NO_RESTOCK">No reponer</SelectItem>
                                                    <SelectItem value="WASTE">Registrar como desperdicio</SelectItem>
                                                </SelectContent>
                                            </Select>
                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />
                            ))}
                        </div>
                    )}

                    <FormField
                        control={form.control}
                        name="reason"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Motivo</FormLabel>
                                <FormControl>
                                    <Textarea {...field} className="resize-none" placeholder="Motivo operativo de cancelacion total" />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <div className="flex justify-end">
                        <Button type="submit" variant="destructive" disabled={cancelOrder.isPending}>
                            {cancelOrder.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                            Cancelar orden
                        </Button>
                    </div>
                </form>
            </Form>
        </DialogShell>
    )
}
