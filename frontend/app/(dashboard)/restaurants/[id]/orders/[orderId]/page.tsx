"use client"

import Link from "next/link"
import { useParams } from "next/navigation"
import { ArrowLeft, Ban, CheckCircle2, CreditCard, Plus, RefreshCw, RotateCcw, Utensils } from "lucide-react"
import { useCallback, useMemo, useState } from "react"

import { CancelOrderDialog } from "@/components/modules/orders/cancel-order-dialog"
import { CancelOrderItemDialog } from "@/components/modules/orders/cancel-order-item-dialog"
import { AddOrderItemsSheet } from "@/components/modules/orders/add-order-items-sheet"
import { RefundDialog } from "@/components/modules/orders/refund-dialog"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { OrderItemDetailResponse } from "@/lib/api-types"
import { formatCurrency } from "@/lib/formatters"
import { useNow } from "@/lib/hooks/use-now"
import { useCompleteOrder, useOrderDetail, useReactivatePayment } from "@/lib/hooks/use-orders"

export default function OrderDetailPage() {
    const params = useParams<{ orderId: string }>()
    const { restaurantId } = useRestaurantContext()
    const orderId = params.orderId
    const { data: order, isLoading } = useOrderDetail(orderId)
    const completeOrder = useCompleteOrder()
    const reactivatePayment = useReactivatePayment()
    const [cancelItem, setCancelItem] = useState<OrderItemDetailResponse | null>(null)
    const [cancelOrderOpen, setCancelOrderOpen] = useState(false)
    const [refundOpen, setRefundOpen] = useState(false)
    const [completeOpen, setCompleteOpen] = useState(false)
    const [addItemsOpen, setAddItemsOpen] = useState(false)
    const now = useNow()
    const itemTicketStatus = useCallback((item: OrderItemDetailResponse) =>
        order?.kitchenTickets.find((ticket) =>
            ticket.lines.some((line) => line.orderItemId === item.id && line.activeQuantity > 0)
        )?.status, [order?.kitchenTickets])
    const requiresDisposition = useCallback((item: OrderItemDetailResponse) => {
        const status = itemTicketStatus(item)
        return status === "PREPARING" || status === "READY"
    }, [itemTicketStatus])

    const paidTotal = useMemo(
        () => (order?.payments ?? [])
            .filter((payment) => payment.status === "RECORDED")
            .reduce((sum, payment) => sum + payment.amount, 0),
        [order?.payments]
    )
    const netCollected = Math.max(paidTotal - (order?.refundedTotalSnapshot ?? 0), 0)

    if (isLoading || !order) {
        return <div className="border bg-muted/20 p-10 text-center text-sm text-muted-foreground">Cargando orden...</div>
    }

    const canComplete = order.orderStatus === "OPEN"
        && order.kitchenStatus === "READY"
        && order.paymentStatus === "PAID"
        && order.refundDueSnapshot === 0
        && order.items.some((item) => item.activeQuantity > 0)
    const canCancel = order.orderStatus !== "COMPLETED" && order.orderStatus !== "CANCELED"
    const canRegisterPayment = order.orderStatus !== "CANCELED"
        && order.orderStatus !== "COMPLETED"
        && order.paymentStatus !== "PAID"
        && order.paymentStatus !== "REFUNDED"
        && order.paymentStatus !== "REFUND_PENDING"
    const paymentExpirationProcessing = order.orderStatus === "AWAITING_PAYMENT"
        && !order.paymentExpired
        && order.paymentExpiresAt !== null
        && new Date(order.paymentExpiresAt).getTime() <= now

    return (
        <div className="space-y-6 pb-12">
            <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                <div>
                    <Button variant="ghost" size="sm" asChild className="mb-2 px-0">
                        <Link href={`/restaurants/${restaurantId}/orders`}>
                            <ArrowLeft className="h-4 w-4" />
                            Volver a ordenes
                        </Link>
                    </Button>
                    <h1 className="text-3xl font-bold tracking-tight">Orden {order.displayCode}</h1>
                    <div className="mt-2 flex flex-wrap gap-2">
                        <Badge variant="outline">{order.orderStatus}</Badge>
                        <Badge variant="secondary">{order.kitchenStatus}</Badge>
                        <Badge>{order.paymentStatus}</Badge>
                        <Badge variant="outline">{order.serviceType}</Badge>
                    </div>
                </div>
                <div className="flex flex-wrap gap-2">
                    {(order.orderStatus === "OPEN"
                        || (order.orderStatus === "AWAITING_PAYMENT"
                            && !order.paymentExpired
                            && !paymentExpirationProcessing)) && (
                        <Button variant="outline" onClick={() => setAddItemsOpen(true)}>
                            <Plus className="h-4 w-4" />
                            Añadir items
                        </Button>
                    )}
                    {order.paymentExpired && (
                        <Button
                            variant="outline"
                            disabled={reactivatePayment.isPending}
                            onClick={() => reactivatePayment.mutate(order.id)}
                        >
                            <RefreshCw className="h-4 w-4" />
                            Reactivar
                        </Button>
                    )}
                    {canRegisterPayment && (
                        <Button
                            variant="outline"
                            asChild={!order.paymentExpired && !paymentExpirationProcessing}
                            disabled={order.paymentExpired || paymentExpirationProcessing}
                        >
                            {order.paymentExpired || paymentExpirationProcessing ? (
                                <span><CreditCard className="h-4 w-4" />Pago bloqueado</span>
                            ) : (
                            <Link href={`/restaurants/${restaurantId}/orders/${order.id}/payments`}>
                                <CreditCard className="h-4 w-4" />
                                Registrar pago
                            </Link>
                            )}
                        </Button>
                    )}
                    {order.refundDueSnapshot > 0 && (
                        <Button variant="outline" onClick={() => setRefundOpen(true)}>
                            <RotateCcw className="h-4 w-4" />
                            Registrar devolución
                        </Button>
                    )}
                    {canCancel && (
                        <Button variant="destructive" onClick={() => setCancelOrderOpen(true)}>
                            <Ban className="h-4 w-4" />
                            Cancelar orden
                        </Button>
                    )}
                    <Button disabled={!canComplete} onClick={() => setCompleteOpen(true)}>
                        <CheckCircle2 className="h-4 w-4" />
                        Completar
                    </Button>
                </div>
            </div>

            {order.refundDueSnapshot > 0 && (
                <div className="border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950">
                    Hay una devolucion pendiente por <strong>{formatCurrency(order.refundDueSnapshot)}</strong>.
                    La orden no puede completarse hasta registrar la devolucion.
                </div>
            )}
            {order.paymentExpired && (
                <div className="border border-destructive/40 bg-destructive/5 p-4 text-sm">
                    Esta orden venció y sus reservas fueron liberadas. Reactívala para volver a validar inventario antes de cobrar o editar.
                </div>
            )}
            {paymentExpirationProcessing && (
                <div className="border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950">
                    El vencimiento esta siendo procesado. El pago y la edicion permaneceran bloqueados hasta finalizar.
                </div>
            )}

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
                <Metric label="Total actual" value={formatCurrency(order.totalGrossSnapshot)} />
                <Metric label="Cobrado" value={formatCurrency(paidTotal)} />
                <Metric label="Cobro neto" value={formatCurrency(netCollected)} />
                <Metric label="Devuelto" value={formatCurrency(order.refundedTotalSnapshot)} />
                <Metric label="Propina" value={formatCurrency(order.tipTotalSnapshot)} />
            </div>

            <section className="space-y-3">
                <div className="flex items-center gap-2">
                    <Utensils className="h-5 w-5" />
                    <h2 className="text-lg font-semibold">Items</h2>
                </div>
                {order.items.map((item) => {
                    const ticketStatus = itemTicketStatus(item)
                    return (
                        <div key={item.id} className="border p-4">
                            <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                                <div>
                                    <div className="flex flex-wrap items-center gap-2">
                                        <p className="font-medium">{item.itemNameSnapshot}</p>
                                        {ticketStatus && <Badge variant="secondary">{ticketStatus}</Badge>}
                                    </div>
                                    <p className="mt-1 text-xs text-muted-foreground">
                                        Original {item.quantity} · Canceladas {item.canceledQuantity} · Activas {item.activeQuantity}
                                    </p>
                                    {item.templateSlots.map((slot) => (
                                        <p key={slot.id} className="mt-1 text-xs text-muted-foreground">
                                            {slot.slotNameSnapshot}: {slot.options.map((option) =>
                                                `${option.itemNameSnapshot} x${option.quantity}`
                                            ).join(", ")}
                                        </p>
                                    ))}
                                </div>
                                <div className="flex items-center gap-3">
                                    <p className="font-mono font-medium">{formatCurrency(item.subtotalGrossSnapshot)}</p>
                                    {item.activeQuantity > 0
                                        && (order.orderStatus === "OPEN" || order.orderStatus === "AWAITING_PAYMENT") && (
                                        <Button size="sm" variant="outline" onClick={() => setCancelItem(item)}>
                                            Cancelar unidades
                                        </Button>
                                    )}
                                </div>
                            </div>

                            {item.cancellations.length > 0 && (
                                <div className="mt-3 border-t pt-3">
                                    <p className="mb-2 text-xs font-medium">Historial de cancelaciones</p>
                                    <div className="space-y-1 text-xs text-muted-foreground">
                                        {item.cancellations.map((cancellation) => (
                                            <p key={cancellation.id}>
                                                x{cancellation.quantity} · {cancellation.inventoryDisposition} · {cancellation.reason}
                                            </p>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    )
                })}
            </section>

            <div className="grid gap-6 xl:grid-cols-2">
                <section className="space-y-3">
                    <h2 className="text-lg font-semibold">Tickets de cocina</h2>
                    {order.kitchenTickets.length === 0 ? (
                        <EmptyState text="La orden todavia no tiene tickets de cocina." />
                    ) : order.kitchenTickets.map((ticket) => (
                        <div key={ticket.id} className="border p-4">
                            <div className="flex justify-between gap-3">
                                <p className="font-medium">Ticket {ticket.id.slice(0, 8)}</p>
                                <Badge variant="secondary">{ticket.status}</Badge>
                            </div>
                            <div className="mt-3 space-y-1 text-xs text-muted-foreground">
                                {ticket.lines.map((line) => (
                                    <p key={line.id}>
                                        {line.itemNameSnapshot}: original {line.quantity}, canceladas {line.canceledQuantity}, activas {line.activeQuantity}
                                    </p>
                                ))}
                            </div>
                        </div>
                    ))}
                </section>

                <section className="space-y-3">
                    <h2 className="text-lg font-semibold">Pagos y devoluciones</h2>
                    {order.payments.length === 0 && order.refunds.length === 0 ? (
                        <EmptyState text="No hay movimientos financieros registrados." />
                    ) : (
                        <div className="space-y-2">
                            {order.payments.map((payment) => (
                                <div key={payment.id} className="flex justify-between border p-3 text-sm">
                                    <div>
                                        <p className="font-medium">Pago {payment.id.slice(0, 8)}</p>
                                        <p className="text-xs text-muted-foreground">Caja {payment.cashSessionId.slice(0, 8)}</p>
                                    </div>
                                    <div className="text-right">
                                        <p className="font-mono">{formatCurrency(payment.amount)}</p>
                                        <Badge variant="outline">{payment.status}</Badge>
                                    </div>
                                </div>
                            ))}
                            {order.refunds.map((refund) => (
                                <div key={refund.id} className="flex justify-between border border-amber-300 p-3 text-sm">
                                    <div>
                                        <p className="font-medium">Devolucion {refund.id.slice(0, 8)}</p>
                                        <p className="text-xs text-muted-foreground">{refund.reason}</p>
                                    </div>
                                    <div className="text-right">
                                        <p className="font-mono">-{formatCurrency(refund.amount)}</p>
                                        <Badge variant="outline">{refund.status}</Badge>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </section>
            </div>

            {cancelItem && (
                <CancelOrderItemDialog
                    orderId={order.id}
                    item={cancelItem}
                    requiresDisposition={requiresDisposition(cancelItem)}
                    open
                    onOpenChange={(open) => { if (!open) setCancelItem(null) }}
                />
            )}
            <CancelOrderDialog
                order={order}
                open={cancelOrderOpen}
                onOpenChange={setCancelOrderOpen}
                requiresDisposition={requiresDisposition}
            />
            <RefundDialog order={order} open={refundOpen} onOpenChange={setRefundOpen} />
            <AddOrderItemsSheet order={order} open={addItemsOpen} onOpenChange={setAddItemsOpen} />
            <ConfirmDialog
                open={completeOpen}
                onOpenChange={setCompleteOpen}
                title="Completar orden"
                description="La orden quedara cerrada. Cocina debe estar lista, el pago completo y no puede haber devoluciones pendientes."
                confirmText="Completar"
                onConfirm={async () => { await completeOrder.mutateAsync(order.id) }}
            />
        </div>
    )
}

function Metric({ label, value }: { label: string; value: string }) {
    return (
        <div className="border p-4">
            <p className="text-xs text-muted-foreground">{label}</p>
            <p className="mt-1 font-mono text-lg font-semibold">{value}</p>
        </div>
    )
}

function EmptyState({ text }: { text: string }) {
    return <div className="border border-dashed bg-muted/20 p-6 text-center text-sm text-muted-foreground">{text}</div>
}
