"use client"

import { useMemo, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { CheckCircle2, CreditCard } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { formatCurrency, formatPriceDisplay, parsePriceInput } from "@/lib/formatters"
import { useOrderDetail, usePaymentMethods, useRegisterPayment } from "@/lib/hooks/use-orders"

export default function OrderPaymentPage() {
    const params = useParams<{ orderId: string }>()
    const { restaurantId } = useRestaurantContext()
    const orderId = params.orderId
    const { data: order, isLoading } = useOrderDetail(orderId)
    const { data: paymentMethods } = usePaymentMethods()
    const registerPayment = useRegisterPayment()
    const activeMethods = useMemo(
        () => paymentMethods?.filter((method) => method.isActive) ?? [],
        [paymentMethods]
    )
    const [paymentMethodId, setPaymentMethodId] = useState("")
    const [amount, setAmount] = useState("")
    const [tipAmount, setTipAmount] = useState("")
    const [externalReference, setExternalReference] = useState("")
    const [notes, setNotes] = useState("")

    const paidAmount = useMemo(() => {
        return (order?.payments ?? [])
            .filter((payment) => payment.status === "RECORDED")
            .reduce((sum, payment) => sum + payment.amount, 0)
    }, [order?.payments])
    const total = order?.totalGrossSnapshot ?? 0
    const remaining = Math.max(total - paidAmount, 0)
    const selectedMethod = activeMethods.find((method) => method.id === paymentMethodId)
    const remainingDisplay = remaining > 0 ? formatPriceDisplay(String(remaining)) : ""
    const effectiveAmountDisplay = amount || remainingDisplay
    const effectiveAmount = parsePriceInput(effectiveAmountDisplay)
    const effectiveTipAmount = parsePriceInput(tipAmount)
    const isPaid = order?.paymentStatus === "PAID" || remaining <= 0

    const submit = async () => {
        await registerPayment.mutateAsync({
            orderId,
            request: {
                paymentMethodId,
                amount: effectiveAmount,
                tipAmount: effectiveTipAmount,
                externalReference: externalReference || undefined,
                notes: notes || undefined,
            },
        })
        setAmount("")
        setTipAmount("")
        setExternalReference("")
        setNotes("")
    }

    const canSubmit = !!paymentMethodId
        && effectiveAmount > 0
        && !isPaid
        && (!selectedMethod?.requiresReference || externalReference.trim().length > 0)
        && !registerPayment.isPending

    const handleMoneyChange = (setter: (value: string) => void) => (value: string) => {
        setter(formatPriceDisplay(value))
    }

    return (
        <div className="mx-auto max-w-5xl space-y-6 pb-10">
            <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Registrar pago</h1>
                    <p className="mt-1 text-muted-foreground">
                        Cobro de orden prepaga. Puedes registrar uno o varios pagos hasta completar el total.
                    </p>
                </div>
                <Button variant="outline" asChild>
                    <Link href={`/restaurants/${restaurantId}/orders`}>Volver a ordenes</Link>
                </Button>
            </div>

            {isLoading || !order ? (
                <div className="border bg-muted/30 p-8 text-center text-sm text-muted-foreground">Cargando orden...</div>
            ) : (
                <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
                    <Card>
                        <CardHeader>
                            <CardTitle className="flex items-center justify-between gap-3">
                                Orden {order.displayCode}
                                <Badge>{order.paymentStatus}</Badge>
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-5">
                            <div className="grid gap-3 sm:grid-cols-3">
                                <div className="border p-4">
                                    <p className="text-xs text-muted-foreground">Total</p>
                                    <p className="mt-1 font-mono text-xl font-semibold">{formatCurrency(total)}</p>
                                </div>
                                <div className="border p-4">
                                    <p className="text-xs text-muted-foreground">Pagado</p>
                                    <p className="mt-1 font-mono text-xl font-semibold">{formatCurrency(paidAmount)}</p>
                                </div>
                                <div className="border p-4">
                                    <p className="text-xs text-muted-foreground">Restante</p>
                                    <p className="mt-1 font-mono text-xl font-semibold">{formatCurrency(remaining)}</p>
                                </div>
                            </div>

                            <div className="space-y-2">
                                <p className="text-sm font-medium">Items</p>
                                {order.items.map((item) => (
                                    <div key={item.id} className="flex justify-between gap-3 border p-3 text-sm">
                                        <div>
                                            <p className="font-medium">{item.itemNameSnapshot}</p>
                                            <p className="text-xs text-muted-foreground">x{item.quantity}</p>
                                        </div>
                                        <p className="font-mono">{formatCurrency(item.subtotalGrossSnapshot)}</p>
                                    </div>
                                ))}
                            </div>

                            {!!order.payments.length && (
                                <div className="space-y-2">
                                    <p className="text-sm font-medium">Pagos registrados</p>
                                    {order.payments.map((payment) => (
                                        <div key={payment.id} className="flex justify-between gap-3 border p-3 text-sm">
                                            <div>
                                                <p className="font-medium">{formatCurrency(payment.amount)}</p>
                                                <p className="text-xs text-muted-foreground">
                                                    Propina {formatCurrency(payment.tipAmount)}
                                                </p>
                                            </div>
                                            <Badge variant="outline">{payment.status}</Badge>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle className="flex items-center gap-2 text-lg">
                                {isPaid ? <CheckCircle2 className="h-5 w-5 text-emerald-600" /> : <CreditCard className="h-5 w-5" />}
                                {isPaid ? "Orden pagada" : "Nuevo pago"}
                            </CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            {isPaid ? (
                                <div className="space-y-4">
                                    <p className="text-sm text-muted-foreground">
                                        El pago esta completo. En prepago, el backend cambia la orden a OPEN y genera el ticket de cocina.
                                    </p>
                                    <Button className="w-full" asChild>
                                        <Link href={`/restaurants/${restaurantId}/orders`}>Ir a ordenes</Link>
                                    </Button>
                                </div>
                            ) : (
                                <>
                                    <div className="grid gap-3">
                                        <Label>Metodo</Label>
                                        <Select value={paymentMethodId} onValueChange={setPaymentMethodId}>
                                            <SelectTrigger><SelectValue placeholder="Selecciona metodo" /></SelectTrigger>
                                            <SelectContent>
                                                {activeMethods.map((method) => (
                                                    <SelectItem key={method.id} value={method.id}>
                                                        {method.name}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    </div>
                                    <div className="grid gap-3">
                                        <Label>Monto aplicado</Label>
                                        <div className="relative">
                                            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">$</span>
                                            <Input
                                                inputMode="decimal"
                                                value={effectiveAmountDisplay}
                                                onChange={(event) => handleMoneyChange(setAmount)(event.target.value)}
                                                className="pl-7"
                                            />
                                        </div>
                                    </div>
                                    <div className="grid gap-3">
                                        <Label>Propina</Label>
                                        <div className="relative">
                                            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">$</span>
                                            <Input
                                                inputMode="decimal"
                                                value={tipAmount}
                                                onChange={(event) => handleMoneyChange(setTipAmount)(event.target.value)}
                                                placeholder="0"
                                                className="pl-7"
                                            />
                                        </div>
                                    </div>
                                    {selectedMethod?.requiresReference && (
                                        <div className="grid gap-3">
                                            <Label>Referencia</Label>
                                            <Input value={externalReference} onChange={(event) => setExternalReference(event.target.value)} />
                                        </div>
                                    )}
                                    <Textarea value={notes} onChange={(event) => setNotes(event.target.value)} placeholder="Notas del pago" />
                                    <Button className="w-full" disabled={!canSubmit} onClick={submit}>
                                        Registrar pago
                                    </Button>
                                    {!activeMethods.length && (
                                        <p className="text-xs text-destructive">Configura al menos un metodo de pago activo.</p>
                                    )}
                                </>
                            )}
                        </CardContent>
                    </Card>
                </div>
            )}
        </div>
    )
}
