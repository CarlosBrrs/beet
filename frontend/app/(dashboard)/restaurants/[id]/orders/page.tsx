"use client"

import { type ReactNode, useState } from "react"
import Link from "next/link"
import { CheckCircle2, CreditCard, Eye, RefreshCw, Search, Settings2 } from "lucide-react"

import { PrepaidSettingsDialog } from "@/components/modules/orders/prepaid-settings-dialog"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ListPagination } from "@/components/shared/list-pagination"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { useDebounce } from "@/lib/hooks/use-debounce"
import { useNow } from "@/lib/hooks/use-now"
import { useCompleteOrder, useOrders, useReactivatePayment } from "@/lib/hooks/use-orders"
import { useMyRestaurants } from "@/lib/hooks/use-my-restaurants"
import { formatCurrency } from "@/lib/formatters"
import { OrderResponse, OrderStatus, ServiceType } from "@/lib/api-types"

const ALL = "ALL"

export default function OrdersPage() {
    const { restaurantId } = useRestaurantContext()
    const [search, setSearch] = useState("")
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(20)
    const [orderStatus, setOrderStatus] = useState<string>("OPEN")
    const [serviceType, setServiceType] = useState<string>(ALL)
    const [sort, setSort] = useState("createdAt,desc")
    const [orderToComplete, setOrderToComplete] = useState<OrderResponse | null>(null)
    const [quickView, setQuickView] = useState<"OPEN" | "PENDING" | "EXPIRED" | "HISTORY">("OPEN")
    const [settingsOpen, setSettingsOpen] = useState(false)
    const now = useNow()
    const debouncedSearch = useDebounce(search, 300)
    const completeOrder = useCompleteOrder()
    const reactivatePayment = useReactivatePayment()
    const { data: restaurants } = useMyRestaurants()
    const restaurant = restaurants?.find((candidate) => candidate.id === restaurantId)
    const { data, isLoading } = useOrders({
        page,
        size,
        sort,
        search: debouncedSearch,
        orderStatus: orderStatus === ALL ? undefined : orderStatus as OrderStatus,
        serviceType: serviceType === ALL ? undefined : serviceType as ServiceType,
        paymentPendingState: quickView === "PENDING"
            ? "ACTIVE"
            : quickView === "EXPIRED" ? "EXPIRED" : undefined,
    })

    return (
        <div className="space-y-6 pb-10">
            <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Ordenes</h1>
                    <p className="mt-1 text-muted-foreground">Busqueda, filtros y paginacion resueltos desde backend.</p>
                </div>
                {restaurant?.operationMode === "PREPAID" && (
                    <Button variant="outline" onClick={() => setSettingsOpen(true)}>
                        <Settings2 className="h-4 w-4" />
                        Configuracion prepago
                    </Button>
                )}
            </div>

            <div className="flex flex-wrap gap-2">
                <QuickViewButton active={quickView === "OPEN"} onClick={() => {
                    setQuickView("OPEN"); setOrderStatus("OPEN"); setPage(0)
                }}>Abiertas</QuickViewButton>
                <QuickViewButton active={quickView === "PENDING"} onClick={() => {
                    setQuickView("PENDING"); setOrderStatus("AWAITING_PAYMENT"); setPage(0)
                }}>Pendientes de pago</QuickViewButton>
                <QuickViewButton active={quickView === "EXPIRED"} onClick={() => {
                    setQuickView("EXPIRED"); setOrderStatus("AWAITING_PAYMENT"); setPage(0)
                }}>Vencidas con atencion</QuickViewButton>
                <QuickViewButton active={quickView === "HISTORY"} onClick={() => {
                    setQuickView("HISTORY"); setOrderStatus(ALL); setPage(0)
                }}>Historico</QuickViewButton>
            </div>

            <div className="grid gap-3 md:grid-cols-[1fr_180px_180px_180px]">
                <div className="relative">
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                        value={search}
                        onChange={(event) => { setSearch(event.target.value); setPage(0) }}
                        placeholder="Buscar por codigo, cliente o notas..."
                        className="pl-9"
                    />
                </div>
                <Select value={orderStatus} onValueChange={(value) => {
                    setOrderStatus(value)
                    setQuickView(value === "OPEN" ? "OPEN" : "HISTORY")
                    setPage(0)
                }}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                        <SelectItem value={ALL}>Todos</SelectItem>
                        <SelectItem value="DRAFT">Draft</SelectItem>
                        <SelectItem value="AWAITING_PAYMENT">Awaiting payment</SelectItem>
                        <SelectItem value="OPEN">Open</SelectItem>
                        <SelectItem value="COMPLETED">Completed</SelectItem>
                        <SelectItem value="CANCELED">Canceled</SelectItem>
                    </SelectContent>
                </Select>
                <Select value={serviceType} onValueChange={(value) => { setServiceType(value); setPage(0) }}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                        <SelectItem value={ALL}>Todos</SelectItem>
                        <SelectItem value="DINE_IN">Mesa</SelectItem>
                        <SelectItem value="TAKEOUT">Para llevar</SelectItem>
                        <SelectItem value="DELIVERY">Domicilio</SelectItem>
                    </SelectContent>
                </Select>
                <Select value={sort} onValueChange={(value) => { setSort(value); setPage(0) }}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                        <SelectItem value="createdAt,desc">Mas recientes</SelectItem>
                        <SelectItem value="createdAt,asc">Mas antiguas</SelectItem>
                        <SelectItem value="totalGross,desc">Mayor total</SelectItem>
                        <SelectItem value="totalGross,asc">Menor total</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            <div className="overflow-hidden border">
                <table className="w-full text-sm">
                    <thead>
                        <tr className="border-b bg-muted/50 text-muted-foreground">
                            <th className="h-11 px-4 text-left font-medium">Orden</th>
                            <th className="h-11 px-4 text-center font-medium">Servicio</th>
                            <th className="h-11 px-4 text-center font-medium">Estado</th>
                            <th className="h-11 px-4 text-center font-medium">Cocina</th>
                            <th className="h-11 px-4 text-center font-medium">Pago</th>
                            <th className="h-11 px-4 text-right font-medium">Saldo</th>
                            <th className="h-11 px-4 text-right font-medium">Total</th>
                            <th className="h-11 px-4 text-right font-medium">Accion rapida</th>
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr><td colSpan={8} className="p-8 text-center text-muted-foreground">Cargando ordenes...</td></tr>
                        ) : (data?.content ?? []).length === 0 ? (
                            <tr><td colSpan={8} className="p-8 text-center text-muted-foreground">No hay ordenes para estos filtros.</td></tr>
                        ) : data!.content.map((order) => (
                            <tr key={order.id} className="border-b last:border-0">
                                <td className="p-4">
                                    <p className="font-medium">{order.customerName || "Sin cliente"}</p>
                                    <p className="text-xs text-muted-foreground">{order.displayCode}</p>
                                </td>
                                <td className="p-4 text-center">{order.serviceType}</td>
                                <td className="p-4 text-center"><Badge variant="outline">{order.orderStatus}</Badge></td>
                                <td className="p-4 text-center"><Badge variant="secondary">{order.kitchenStatus}</Badge></td>
                                <td className="p-4 text-center">
                                    <Badge variant={order.paymentExpired ? "destructive" : "default"}>
                                        {order.paymentExpired
                                            ? "VENCIDA"
                                            : paymentDeadlineElapsed(order.paymentExpiresAt, now) ? "PROCESANDO" : order.paymentStatus}
                                    </Badge>
                                    {order.orderStatus === "AWAITING_PAYMENT" && !order.paymentExpired && (
                                        <p className="mt-1 text-xs text-muted-foreground">
                                            {formatExpiration(order.paymentExpiresAt, now)}
                                        </p>
                                    )}
                                </td>
                                <td className="p-4 text-right font-mono">{formatCurrency(order.remainingBalance)}</td>
                                <td className="p-4 text-right font-mono">{formatCurrency(order.totalGrossSnapshot)}</td>
                                <td className="p-4 text-right">
                                    <div className="flex justify-end gap-2">
                                        <Button size="sm" variant="outline" asChild>
                                            <Link href={`/restaurants/${restaurantId}/orders/${order.id}`}>
                                                <Eye className="h-4 w-4" />
                                                Ver
                                            </Link>
                                        </Button>
                                        {canRegisterPayment(order) && (
                                            order.paymentExpired ? (
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    disabled={reactivatePayment.isPending}
                                                    onClick={() => reactivatePayment.mutate(order.id)}
                                                >
                                                    <RefreshCw className="h-4 w-4" />
                                                    Reactivar
                                                </Button>
                                            ) : paymentDeadlineElapsed(order.paymentExpiresAt, now) ? (
                                                <Button size="sm" variant="outline" disabled>
                                                    <RefreshCw className="h-4 w-4" />
                                                    Procesando
                                                </Button>
                                            ) : (
                                                <Button size="sm" variant="outline" asChild>
                                                    <Link href={`/restaurants/${restaurantId}/orders/${order.id}/payments`}>
                                                        <CreditCard className="h-4 w-4" />
                                                        {order.paidTotal > 0 ? "Retomar pago" : "Cobrar"}
                                                    </Link>
                                                </Button>
                                            )
                                        )}
                                        <Button
                                            size="sm"
                                            disabled={!canCompleteOrder(order) || completeOrder.isPending}
                                            title={completionBlockReason(order)}
                                            onClick={() => setOrderToComplete(order)}
                                        >
                                            <CheckCircle2 className="h-4 w-4" />
                                            Completar
                                        </Button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <ListPagination
                totalElements={data?.totalElements ?? 0}
                totalPages={data?.totalPages ?? 0}
                page={page}
                size={size}
                label="orden(es)"
                onPageChange={setPage}
                onSizeChange={(nextSize) => { setSize(nextSize); setPage(0) }}
            />

            <ConfirmDialog
                open={orderToComplete !== null}
                onOpenChange={(open) => {
                    if (!open) setOrderToComplete(null)
                }}
                title="Completar orden"
                description={
                    orderToComplete
                        ? `La orden ${orderToComplete.displayCode} quedara cerrada y no admitira nuevas operaciones.`
                        : ""
                }
                confirmText="Completar"
                onConfirm={async () => {
                    if (!orderToComplete) return
                    await completeOrder.mutateAsync(orderToComplete.id)
                }}
            />
            {restaurant && (
                <PrepaidSettingsDialog
                    restaurant={restaurant}
                    open={settingsOpen}
                    onOpenChange={setSettingsOpen}
                />
            )}
        </div>
    )
}

function QuickViewButton({
    active,
    onClick,
    children,
}: {
    active: boolean
    onClick: () => void
    children: ReactNode
}) {
    return <Button size="sm" variant={active ? "default" : "outline"} onClick={onClick}>{children}</Button>
}

function formatExpiration(value: string | null, now: number) {
    if (!value) return "Sin vencimiento"
    const remaining = new Date(value).getTime() - now
    if (remaining <= 0) return "Procesando vencimiento"
    const minutes = Math.ceil(remaining / 60000)
    return `Vence en ${minutes} min`
}

function paymentDeadlineElapsed(value: string | null, now: number) {
    return value !== null && new Date(value).getTime() <= now
}

function canRegisterPayment(order: OrderResponse) {
    return order.orderStatus !== "CANCELED"
        && order.orderStatus !== "COMPLETED"
        && order.paymentStatus !== "PAID"
        && order.paymentStatus !== "REFUNDED"
        && order.paymentStatus !== "REFUND_PENDING"
}
function canCompleteOrder(order: OrderResponse) {
    return order.orderStatus === "OPEN"
        && order.kitchenStatus === "READY"
        && order.paymentStatus === "PAID"
        && order.refundDueSnapshot === 0
}

function completionBlockReason(order: OrderResponse) {
    if (order.orderStatus !== "OPEN") return "Solo se pueden completar ordenes abiertas."
    if (order.kitchenStatus !== "READY") return "La cocina debe estar en estado READY."
    if (order.paymentStatus !== "PAID") return "La orden debe estar pagada."
    if (order.refundDueSnapshot > 0) return "Hay una devolucion pendiente."
    return "Completar orden"
}
