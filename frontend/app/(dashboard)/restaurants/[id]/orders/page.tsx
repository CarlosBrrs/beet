"use client"

import { useState } from "react"
import Link from "next/link"
import { Search } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ListPagination } from "@/components/shared/list-pagination"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { useDebounce } from "@/lib/hooks/use-debounce"
import { useOrders } from "@/lib/hooks/use-orders"
import { formatCurrency } from "@/lib/formatters"
import { OrderStatus, ServiceType } from "@/lib/api-types"

const ALL = "ALL"

export default function OrdersPage() {
    const { restaurantId } = useRestaurantContext()
    const [search, setSearch] = useState("")
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(20)
    const [orderStatus, setOrderStatus] = useState<string>("OPEN")
    const [serviceType, setServiceType] = useState<string>(ALL)
    const [sort, setSort] = useState("createdAt,desc")
    const debouncedSearch = useDebounce(search, 300)
    const { data, isLoading } = useOrders({
        page,
        size,
        sort,
        search: debouncedSearch,
        orderStatus: orderStatus === ALL ? undefined : orderStatus as OrderStatus,
        serviceType: serviceType === ALL ? undefined : serviceType as ServiceType,
    })

    return (
        <div className="space-y-6 pb-10">
            <div>
                <h1 className="text-3xl font-bold tracking-tight">Ordenes</h1>
                <p className="mt-1 text-muted-foreground">Busqueda, filtros y paginacion resueltos desde backend.</p>
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
                <Select value={orderStatus} onValueChange={(value) => { setOrderStatus(value); setPage(0) }}>
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
                            <th className="h-11 px-4 text-right font-medium">Total</th>
                            <th className="h-11 px-4 text-right font-medium">Accion rapida</th>
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr><td colSpan={7} className="p-8 text-center text-muted-foreground">Cargando ordenes...</td></tr>
                        ) : (data?.content ?? []).length === 0 ? (
                            <tr><td colSpan={7} className="p-8 text-center text-muted-foreground">No hay ordenes para estos filtros.</td></tr>
                        ) : data!.content.map((order) => (
                            <tr key={order.id} className="border-b last:border-0">
                                <td className="p-4">
                                    <p className="font-medium">{order.customerName || "Cliente sin nombre"}</p>
                                    <p className="text-xs text-muted-foreground">{order.displayCode}</p>
                                </td>
                                <td className="p-4 text-center">{order.serviceType}</td>
                                <td className="p-4 text-center"><Badge variant="outline">{order.orderStatus}</Badge></td>
                                <td className="p-4 text-center"><Badge variant="secondary">{order.kitchenStatus}</Badge></td>
                                <td className="p-4 text-center"><Badge>{order.paymentStatus}</Badge></td>
                                <td className="p-4 text-right font-mono">{formatCurrency(order.totalGrossSnapshot)}</td>
                                <td className="p-4 text-right">
                                    {order.paymentStatus === "PAID" ? (
                                        <span className="text-xs text-muted-foreground">Pagada</span>
                                    ) : (
                                        <Button size="sm" variant="outline" asChild>
                                            <Link href={`/restaurants/${restaurantId}/orders/${order.id}/payments`}>
                                                Cobrar
                                            </Link>
                                        </Button>
                                    )}
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
        </div>
    )
}
