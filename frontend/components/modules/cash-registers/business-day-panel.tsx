"use client"

import { CalendarClock, Loader2, LockKeyhole, RotateCcw } from "lucide-react"
import { useMemo, useState } from "react"

import { Can } from "@/components/shared/can"
import { DialogShell } from "@/components/shared/dialog-shell"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import {
    useBusinessDays,
    useCloseBusinessDay,
    useCurrentBusinessDay,
    useOpenBusinessDay,
    useReopenBusinessDay,
} from "@/lib/hooks/use-cash-operations"

export function BusinessDayPanel() {
    const { data: current, isLoading } = useCurrentBusinessDay()
    const { data: days = [] } = useBusinessDays()
    const openDay = useOpenBusinessDay()
    const closeDay = useCloseBusinessDay()
    const reopenDay = useReopenBusinessDay()
    const [closeOpen, setCloseOpen] = useState(false)
    const [reopenOpen, setReopenOpen] = useState(false)
    const [notes, setNotes] = useState("")
    const latestClosed = useMemo(() => days.find((day) => day.status === "CLOSED"), [days])

    if (isLoading) {
        return <div className="h-24 animate-pulse border bg-muted/30" />
    }

    return (
        <>
            <section className="border">
                <div className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-start gap-3">
                        <div className="flex h-9 w-9 items-center justify-center border bg-muted/40">
                            <CalendarClock className="h-4 w-4" />
                        </div>
                        <div>
                            <div className="flex items-center gap-2">
                                <h2 className="text-sm font-semibold">Dia operativo</h2>
                                <Badge variant={current ? "outline" : "secondary"}>
                                    {current ? "Abierto" : "Cerrado"}
                                </Badge>
                            </div>
                            <p className="mt-1 text-xs text-muted-foreground">
                                {current
                                    ? `${current.businessDate} · ${current.timeZone}`
                                    : "Abre el dia antes de crear ordenes, cobrar o abrir cajas."}
                            </p>
                        </div>
                    </div>
                    <Can I="MANAGE" a="CASH">
                        <div className="flex gap-2">
                            {current ? (
                                <Button variant="outline" onClick={() => setCloseOpen(true)}>
                                    <LockKeyhole className="h-4 w-4" />
                                    Cerrar dia
                                </Button>
                            ) : (
                                <>
                                    <Button onClick={() => openDay.mutate()} disabled={openDay.isPending}>
                                        {openDay.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                                        Abrir dia
                                    </Button>
                                    {latestClosed && (
                                        <Button variant="outline" onClick={() => setReopenOpen(true)}>
                                            <RotateCcw className="h-4 w-4" />
                                            Reabrir
                                        </Button>
                                    )}
                                </>
                            )}
                        </div>
                    </Can>
                </div>
            </section>

            <DialogShell
                open={closeOpen}
                onOpenChange={setCloseOpen}
                title="Cerrar dia operativo"
                description="Todas las cajas deben estar arqueadas y no pueden quedar ordenes o devoluciones pendientes."
            >
                {current && (
                    <div className="mb-4 grid grid-cols-2 gap-px border bg-border text-sm">
                        <Block label="Sesiones abiertas" value={current.openSessionCount} />
                        <Block label="Ordenes/devoluciones pendientes" value={current.pendingOrderCount} />
                        <Block label="Arqueos faltantes" value={current.missingReconciliationCount} />
                        <Block label="Diferencias sin explicar" value={current.unexplainedDifferenceCount} />
                    </div>
                )}
                <Textarea value={notes} onChange={(event) => setNotes(event.target.value)} placeholder="Notas opcionales" />
                <div className="mt-4 flex justify-end">
                    <Button
                        onClick={() => current && closeDay.mutate(
                            { businessDayId: current.id, notes: notes.trim() || undefined },
                            { onSuccess: () => setCloseOpen(false) }
                        )}
                        disabled={!current || closeDay.isPending}
                    >
                        Cerrar dia
                    </Button>
                </div>
            </DialogShell>

            <DialogShell
                open={reopenOpen}
                onOpenChange={setReopenOpen}
                title="Reabrir dia operativo"
                description="La reapertura conserva el cierre anterior y exige un motivo."
            >
                <Textarea value={notes} onChange={(event) => setNotes(event.target.value)} placeholder="Motivo obligatorio" />
                <div className="mt-4 flex justify-end">
                    <Button
                        onClick={() => latestClosed && reopenDay.mutate(
                            { businessDayId: latestClosed.id, reason: notes.trim() },
                            { onSuccess: () => setReopenOpen(false) }
                        )}
                        disabled={!latestClosed || !notes.trim() || reopenDay.isPending}
                    >
                        Reabrir dia
                    </Button>
                </div>
            </DialogShell>
        </>
    )
}

function Block({ label, value }: { label: string; value: number }) {
    return (
        <div className="bg-background p-3">
            <p className="text-xs text-muted-foreground">{label}</p>
            <p className={value > 0 ? "mt-1 font-semibold text-destructive" : "mt-1 font-semibold"}>{value}</p>
        </div>
    )
}
