import { Info } from "lucide-react"

export default function OrdersActivePage() {
    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-3xl font-bold tracking-tight">Ordenes en vivo</h1>
                <p className="text-muted-foreground mt-1">
                    Monitor operativo para cocina y barra. La vista detallada llegara en fases posteriores.
                </p>
            </div>

            <div className="rounded-xl border border-amber-200/70 bg-gradient-to-br from-amber-50 via-amber-100/60 to-amber-50 p-5 text-amber-950 shadow-sm">
                <div className="flex gap-3">
                    <Info className="mt-0.5 h-4 w-4 shrink-0" />
                    <div className="space-y-2">
                        <p className="font-semibold">Nota tecnica: impuestos en ordenes (MVP)</p>
                        <p className="text-sm leading-relaxed">
                            Hoy los precios son brutos (impuesto incluido). El impuesto se deriva de la configuracion
                            activa de restaurant_taxes y, si no hay, del defaultTaxPercentage. No hay desglose por item
                            en la UI todavia.
                        </p>
                        <ul className="text-sm list-disc pl-4 space-y-1">
                            <li>tax_amount = gross * rate / (100 + rate)</li>
                            <li>subtotal_gross_snapshot = suma de subtotales brutos por item</li>
                            <li>order_taxes / order_item_taxes guardan snapshots para historico estable</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    )
}
