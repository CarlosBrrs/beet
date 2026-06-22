import { Badge } from "@/components/ui/badge"
import { ReportOverview } from "@/lib/api-types"
import { formatCurrency } from "@/lib/formatters"

export function ReportKpis({ overview }: { overview: ReportOverview }) {
    const metrics = [
        ["Ventas brutas", formatCurrency(overview.grossSales)],
        ["Ventas completadas", formatCurrency(overview.completedSales)],
        ["Cobrado neto", formatCurrency(overview.netCollected)],
        ["Ticket promedio", formatCurrency(overview.averageTicket)],
        ["Ordenes completadas", String(overview.completedOrders)],
        ["Propinas", formatCurrency(overview.tips)],
    ]

    return (
        <section>
            <div className="mb-3 flex items-center gap-2">
                <h2 className="text-sm font-semibold">Resumen</h2>
                {overview.provisional && <Badge variant="secondary">Provisional</Badge>}
            </div>
            <div className="grid border md:grid-cols-3 xl:grid-cols-6">
                {metrics.map(([label, value]) => (
                    <div key={label} className="border-b p-4 md:border-r xl:border-b-0">
                        <p className="text-xs text-muted-foreground">{label}</p>
                        <p className="mt-1 text-lg font-semibold">{value}</p>
                    </div>
                ))}
            </div>
        </section>
    )
}
