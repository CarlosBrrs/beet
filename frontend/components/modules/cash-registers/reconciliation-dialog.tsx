"use client"

import { Loader2 } from "lucide-react"

import { DialogShell } from "@/components/shared/dialog-shell"
import { CashSessionListResponse } from "@/lib/api-types"
import { formatCurrency } from "@/lib/formatters"
import { useCashSessionReconciliation } from "@/lib/hooks/use-cash-operations"

export function ReconciliationDialog({
    session,
    open,
    onOpenChange,
}: {
    session: CashSessionListResponse | null
    open: boolean
    onOpenChange: (open: boolean) => void
}) {
    const { data, isLoading } = useCashSessionReconciliation(
        session?.id ?? null,
        open,
        session?.restaurantId
    )

    return (
        <DialogShell
            open={open}
            onOpenChange={onOpenChange}
            title="Resumen de arqueo"
            description={session ? `${session.restaurantName} - ${session.cashRegisterName}` : undefined}
        >
            {isLoading ? (
                <div className="flex h-28 items-center justify-center">
                    <Loader2 className="h-5 w-5 animate-spin" />
                </div>
            ) : data?.blind ? (
                <p className="border bg-muted/30 p-4 text-sm">
                    Conteo ciego activo. Los totales se revelan despues de cerrar la sesion.
                </p>
            ) : data ? (
                <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-px border bg-border text-sm">
                        <Amount label="Apertura" value={data.openingAmount} />
                        <Amount label="Esperado" value={data.expectedCash} />
                        <Amount label="Contado" value={data.countedCash} />
                        <Amount label="Diferencia" value={data.differenceAmount} />
                        <Amount label="Entradas" value={data.cashInTotal} />
                        <Amount label="Salidas" value={data.cashOutTotal} />
                    </div>
                    <div className="border">
                        {data.paymentTotals.length === 0 ? (
                            <p className="p-4 text-sm text-muted-foreground">Sin pagos registrados.</p>
                        ) : data.paymentTotals.map((total) => (
                            <div key={total.methodCode} className="grid grid-cols-[1fr_auto] gap-3 border-b p-3 text-sm last:border-0">
                                <div>
                                    <p className="font-medium">{total.methodName}</p>
                                    <p className="text-xs text-muted-foreground">
                                        {total.paymentCount} pago(s) · propina {formatCurrency(Number(total.tipAmount))}
                                    </p>
                                </div>
                                <strong>{formatCurrency(Number(total.netAmount))}</strong>
                            </div>
                        ))}
                    </div>
                    {data.differenceReason && (
                        <p className="text-sm text-muted-foreground">{data.differenceReason}</p>
                    )}
                </div>
            ) : null}
        </DialogShell>
    )
}

function Amount({ label, value }: { label: string; value: number | null }) {
    return (
        <div className="bg-background p-3">
            <p className="text-xs text-muted-foreground">{label}</p>
            <p className="mt-1 font-semibold">{value === null ? "-" : formatCurrency(Number(value))}</p>
        </div>
    )
}
