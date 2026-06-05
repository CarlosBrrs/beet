"use client"

import { useState } from "react"
import { ChefHat, Check, Flame } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ListPagination } from "@/components/shared/list-pagination"
import { KitchenTicketStatus } from "@/lib/api-types"
import { useKitchenTickets, useUpdateKitchenTicketStatus } from "@/lib/hooks/use-orders"

const ALL = "ALL"

export default function OrdersActivePage() {
    const [status, setStatus] = useState<string>("PENDING")
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(20)
    const { data, isLoading } = useKitchenTickets({
        page,
        size,
        status: status === ALL ? undefined : status as KitchenTicketStatus,
    })
    const updateStatus = useUpdateKitchenTicketStatus()

    return (
        <div className="space-y-6 pb-10">
            <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Live Orders (KDS)</h1>
                    <p className="mt-1 text-muted-foreground">Cocina trabaja por tickets. Cada envio nuevo entra como ticket separado.</p>
                </div>
                <Select value={status} onValueChange={(value) => { setStatus(value); setPage(0) }}>
                    <SelectTrigger className="w-52"><SelectValue /></SelectTrigger>
                    <SelectContent>
                        <SelectItem value={ALL}>Todos</SelectItem>
                        <SelectItem value="PENDING">Pendientes</SelectItem>
                        <SelectItem value="PREPARING">Preparando</SelectItem>
                        <SelectItem value="READY">Listos</SelectItem>
                        <SelectItem value="CANCELED">Cancelados</SelectItem>
                    </SelectContent>
                </Select>
            </div>

            <div className="border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900">
                Pendiente no bloqueante: esta vista consulta tickets por HTTP paginado. Migrar KDS a SSE para recibir nuevos tickets y cambios de estado en tiempo real sin refrescar.
            </div>

            <div className="grid gap-4 lg:grid-cols-2 2xl:grid-cols-3">
                {isLoading ? (
                    Array.from({ length: 6 }).map((_, index) => <div key={index} className="h-48 animate-pulse border bg-muted/40" />)
                ) : (data?.content ?? []).length === 0 ? (
                    <div className="col-span-full border border-dashed bg-muted/20 p-12 text-center text-sm text-muted-foreground">
                        No hay tickets para este filtro.
                    </div>
                ) : data!.content.map((ticket) => (
                    <div key={ticket.id} className="border bg-background p-4">
                        <div className="flex items-start justify-between gap-3">
                            <div>
                                <p className="flex items-center gap-2 font-semibold">
                                    <ChefHat className="h-4 w-4" />
                                    Orden {ticket.orderDisplayCode ?? ticket.orderId.slice(0, 8)}
                                </p>
                                <p className="mt-1 text-xs text-muted-foreground">
                                    Ticket {ticket.id.slice(0, 8)} · {new Date(ticket.sentAt).toLocaleString()}
                                </p>
                            </div>
                            <Badge variant={ticket.status === "READY" ? "default" : "secondary"}>{ticket.status}</Badge>
                        </div>

                        <div className="mt-4 space-y-2">
                            {ticket.lines.map((line) => (
                                <div key={line.id} className="flex justify-between gap-3 border p-3 text-sm">
                                    <span className="font-medium">{line.itemNameSnapshot}</span>
                                    <span className="font-mono">x{line.quantity}</span>
                                </div>
                            ))}
                        </div>

                        <div className="mt-4 flex justify-end gap-2">
                            <Button
                                size="sm"
                                variant="outline"
                                disabled={ticket.status !== "PENDING"}
                                onClick={() => updateStatus.mutate({ ticketId: ticket.id, status: "PREPARING" })}
                            >
                                <Flame className="mr-2 h-4 w-4" />
                                Iniciar
                            </Button>
                            <Button
                                size="sm"
                                disabled={ticket.status !== "PREPARING"}
                                onClick={() => updateStatus.mutate({ ticketId: ticket.id, status: "READY" })}
                            >
                                <Check className="mr-2 h-4 w-4" />
                                Listo
                            </Button>
                        </div>
                    </div>
                ))}
            </div>

            <ListPagination
                totalElements={data?.totalElements ?? 0}
                totalPages={data?.totalPages ?? 0}
                page={page}
                size={size}
                label="ticket(s)"
                onPageChange={setPage}
                onSizeChange={(nextSize) => { setSize(nextSize); setPage(0) }}
            />
        </div>
    )
}
