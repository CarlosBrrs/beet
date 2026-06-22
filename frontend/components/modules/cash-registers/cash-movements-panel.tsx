"use client"

import { ArrowDownToLine, ArrowUpFromLine, Ban, Loader2, Plus } from "lucide-react"
import { useState } from "react"

import { Can } from "@/components/shared/can"
import { DialogShell } from "@/components/shared/dialog-shell"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { CashMovementDirection, CashMovementReason, CashMovementResponse } from "@/lib/api-types"
import { formatCurrency, formatPriceDisplay, parsePriceInput } from "@/lib/formatters"
import {
    useCashMovements,
    useRecordCashMovement,
    useVoidCashMovement,
} from "@/lib/hooks/use-cash-operations"
import { useActiveCashSession } from "@/lib/hooks/use-cash-sessions"

const reasons: { value: CashMovementReason; label: string }[] = [
    { value: "CHANGE_FUND", label: "Fondo de cambio" },
    { value: "SAFE_DROP", label: "Retiro a caja fuerte" },
    { value: "PETTY_EXPENSE", label: "Gasto menor" },
    { value: "CORRECTION", label: "Correccion" },
    { value: "OTHER", label: "Otro" },
]

export function CashMovementsPanel() {
    const { data: session } = useActiveCashSession()
    const { data: movements = [], isLoading } = useCashMovements(session?.id ?? null)
    const record = useRecordCashMovement(session?.id ?? "")
    const voidMovement = useVoidCashMovement(session?.id ?? "")
    const [formOpen, setFormOpen] = useState(false)
    const [voidTarget, setVoidTarget] = useState<CashMovementResponse | null>(null)
    const [direction, setDirection] = useState<CashMovementDirection>("IN")
    const [reason, setReason] = useState<CashMovementReason>("CHANGE_FUND")
    const [amount, setAmount] = useState("")
    const [notes, setNotes] = useState("")

    if (!session) return null

    const reset = () => {
        setDirection("IN")
        setReason("CHANGE_FUND")
        setAmount("")
        setNotes("")
    }

    return (
        <>
            <section className="space-y-3">
                <div className="flex items-center justify-between">
                    <div>
                        <h2 className="text-lg font-semibold">Movimientos de efectivo</h2>
                        <p className="text-xs text-muted-foreground">Entradas y salidas distintas de ventas.</p>
                    </div>
                    <Can I="PROCESS" a="CASH">
                        <Button variant="outline" onClick={() => setFormOpen(true)}>
                            <Plus className="h-4 w-4" />
                            Registrar
                        </Button>
                    </Can>
                </div>
                <div className="border">
                    {isLoading ? (
                        <div className="flex h-20 items-center justify-center">
                            <Loader2 className="h-4 w-4 animate-spin" />
                        </div>
                    ) : movements.length === 0 ? (
                        <p className="p-6 text-center text-sm text-muted-foreground">No hay movimientos registrados.</p>
                    ) : (
                        movements.map((movement) => (
                            <div key={movement.id} className="flex items-center gap-3 border-b p-3 last:border-0">
                                <div className="flex h-8 w-8 items-center justify-center border">
                                    {movement.direction === "IN"
                                        ? <ArrowDownToLine className="h-4 w-4 text-emerald-600" />
                                        : <ArrowUpFromLine className="h-4 w-4 text-rose-600" />}
                                </div>
                                <div className="min-w-0 flex-1">
                                    <div className="flex items-center gap-2">
                                        <p className="text-sm font-medium">
                                            {reasons.find((item) => item.value === movement.reason)?.label}
                                        </p>
                                        {movement.status === "VOIDED" && <Badge variant="secondary">Anulado</Badge>}
                                    </div>
                                    <p className="truncate text-xs text-muted-foreground">{movement.notes || "Sin nota"}</p>
                                </div>
                                <strong className={movement.direction === "IN" ? "text-emerald-700" : "text-rose-700"}>
                                    {movement.direction === "IN" ? "+" : "-"}{formatCurrency(Number(movement.amount))}
                                </strong>
                                {movement.status === "RECORDED" && (
                                    <Can I="VOID" a="CASH">
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            title="Anular movimiento"
                                            onClick={() => {
                                                setVoidTarget(movement)
                                                setNotes("")
                                            }}
                                        >
                                            <Ban className="h-4 w-4" />
                                        </Button>
                                    </Can>
                                )}
                            </div>
                        ))
                    )}
                </div>
            </section>

            <DialogShell
                open={formOpen}
                onOpenChange={(open) => {
                    setFormOpen(open)
                    if (!open) reset()
                }}
                title="Registrar movimiento"
            >
                <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-3">
                        <Select value={direction} onValueChange={(value) => setDirection(value as CashMovementDirection)}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                                <SelectItem value="IN">Entrada</SelectItem>
                                <SelectItem value="OUT">Salida</SelectItem>
                            </SelectContent>
                        </Select>
                        <Select value={reason} onValueChange={(value) => setReason(value as CashMovementReason)}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                                {reasons.map((item) => (
                                    <SelectItem key={item.value} value={item.value}>{item.label}</SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                    <Input
                        inputMode="decimal"
                        placeholder="Monto"
                        value={amount}
                        onChange={(event) => setAmount(event.target.value)}
                        onBlur={() => setAmount(formatPriceDisplay(amount))}
                    />
                    <Textarea
                        placeholder={reason === "OTHER" || reason === "CORRECTION"
                            ? "Nota obligatoria"
                            : "Nota opcional"}
                        value={notes}
                        onChange={(event) => setNotes(event.target.value)}
                    />
                    <div className="flex justify-end">
                        <Button
                            disabled={!amount || record.isPending
                                || ((reason === "OTHER" || reason === "CORRECTION") && !notes.trim())}
                            onClick={() => record.mutate({
                                direction,
                                reason,
                                amount: parsePriceInput(amount),
                                notes: notes.trim() || undefined,
                            }, { onSuccess: () => setFormOpen(false) })}
                        >
                            Registrar
                        </Button>
                    </div>
                </div>
            </DialogShell>

            <DialogShell
                open={!!voidTarget}
                onOpenChange={(open) => {
                    if (!open) setVoidTarget(null)
                }}
                title="Anular movimiento"
                description="La fila se conserva para auditoria y deja de afectar el arqueo."
            >
                <Textarea placeholder="Motivo obligatorio" value={notes} onChange={(event) => setNotes(event.target.value)} />
                <div className="mt-4 flex justify-end">
                    <Button
                        variant="destructive"
                        disabled={!voidTarget || !notes.trim() || voidMovement.isPending}
                        onClick={() => voidTarget && voidMovement.mutate(
                            { movementId: voidTarget.id, reason: notes.trim() },
                            { onSuccess: () => setVoidTarget(null) }
                        )}
                    >
                        Anular
                    </Button>
                </div>
            </DialogShell>
        </>
    )
}
