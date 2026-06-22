"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2 } from "lucide-react"
import { useEffect, useMemo } from "react"
import { useForm, useWatch } from "react-hook-form"
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
import { OrderDetailResponse } from "@/lib/api-types"
import { formatCurrency, formatPriceDisplay, parsePriceInput } from "@/lib/formatters"
import { useRegisterRefund } from "@/lib/hooks/use-orders"

const schema = z.object({
    paymentId: z.string().min(1, "Selecciona el pago original."),
    amount: z.string().min(1, "Indica el monto a devolver."),
    reason: z.string().trim().min(3, "Indica el motivo de la devolucion."),
    reference: z.string(),
})

type Values = z.infer<typeof schema>

interface RefundDialogProps {
    order: OrderDetailResponse
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function RefundDialog({ order, open, onOpenChange }: RefundDialogProps) {
    const registerRefund = useRegisterRefund(order.id)
    const form = useForm<Values>({
        resolver: zodResolver(schema),
        defaultValues: { paymentId: "", amount: "", reason: "", reference: "" },
    })
    const paymentId = useWatch({ control: form.control, name: "paymentId" })
    const refundableByPayment = useMemo(() => {
        const refunded = order.refunds
            .filter((refund) => refund.paymentId === paymentId && refund.status === "RECORDED")
            .reduce((sum, refund) => sum + refund.amount, 0)
        const payment = order.payments.find((candidate) => candidate.id === paymentId)
        return Math.max((payment?.amount ?? 0) - refunded, 0)
    }, [order.payments, order.refunds, paymentId])

    useEffect(() => {
        if (open) {
            form.reset({
                paymentId: "",
                amount: formatPriceDisplay(String(order.refundDueSnapshot)),
                reason: "",
                reference: "",
            })
        }
    }, [form, open, order.refundDueSnapshot])

    const submit = (values: Values) => {
        const amount = parsePriceInput(values.amount)
        const maximum = Math.min(order.refundDueSnapshot, refundableByPayment)
        if (amount <= 0 || amount > maximum) {
            form.setError("amount", { message: `El maximo permitido es ${formatCurrency(maximum)}.` })
            return
        }
        registerRefund.mutate(
            {
                paymentId: values.paymentId,
                amount,
                reason: values.reason,
                reference: values.reference.trim() || undefined,
            },
            { onSuccess: () => onOpenChange(false) }
        )
    }

    return (
        <DialogShell
            open={open}
            onOpenChange={onOpenChange}
            title="Registrar devolucion"
            description={`Saldo pendiente: ${formatCurrency(order.refundDueSnapshot)}. Requiere una sesion de caja abierta.`}
        >
            <Form {...form}>
                <form onSubmit={form.handleSubmit(submit)} className="space-y-4">
                    <FormField
                        control={form.control}
                        name="paymentId"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Pago original</FormLabel>
                                <Select value={field.value} onValueChange={field.onChange}>
                                    <FormControl>
                                        <SelectTrigger><SelectValue placeholder="Selecciona un pago" /></SelectTrigger>
                                    </FormControl>
                                    <SelectContent>
                                        {order.payments
                                            .filter((payment) => payment.status === "RECORDED")
                                            .map((payment) => (
                                                <SelectItem key={payment.id} value={payment.id}>
                                                    {formatCurrency(payment.amount)} · {payment.id.slice(0, 8)}
                                                </SelectItem>
                                            ))}
                                    </SelectContent>
                                </Select>
                                {paymentId && (
                                    <FormDescription>
                                        Disponible en este pago: {formatCurrency(refundableByPayment)}
                                    </FormDescription>
                                )}
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="amount"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Monto</FormLabel>
                                <FormControl>
                                    <Input
                                        inputMode="decimal"
                                        {...field}
                                        onChange={(event) => field.onChange(formatPriceDisplay(event.target.value))}
                                    />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="reason"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Motivo</FormLabel>
                                <FormControl><Textarea {...field} className="resize-none" /></FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="reference"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Referencia</FormLabel>
                                <FormControl><Input {...field} placeholder="Opcional" /></FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <div className="flex justify-end">
                        <Button type="submit" disabled={registerRefund.isPending}>
                            {registerRefund.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                            Registrar devolucion
                        </Button>
                    </div>
                </form>
            </Form>
        </DialogShell>
    )
}
