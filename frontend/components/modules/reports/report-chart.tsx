"use client"

import {
    Area,
    AreaChart,
    CartesianGrid,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts"

import { ReportSeriesPoint } from "@/lib/api-types"
import { formatCurrency } from "@/lib/formatters"

interface ReportChartProps {
    points: ReportSeriesPoint[]
}

export function ReportChart({ points }: ReportChartProps) {
    if (points.length === 0) {
        return <div className="flex h-72 items-center justify-center border text-sm text-muted-foreground">Sin datos para el rango.</div>
    }

    return (
        <div className="h-72 border p-3">
            <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={points}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="period" tick={{ fontSize: 11 }} />
                    <YAxis
                        tick={{ fontSize: 11 }}
                        width={72}
                        tickFormatter={(value) => new Intl.NumberFormat("es-CO", {
                            notation: "compact",
                            maximumFractionDigits: 1,
                        }).format(Number(value))}
                    />
                    <Tooltip formatter={(value) => formatCurrency(Number(value))} />
                    <Area
                        type="monotone"
                        dataKey="grossSales"
                        name="Ventas brutas"
                        stroke="var(--chart-2)"
                        fill="var(--chart-2)"
                        fillOpacity={0.2}
                    />
                    <Area
                        type="monotone"
                        dataKey="collected"
                        name="Cobrado"
                        stroke="var(--chart-4)"
                        fill="var(--chart-4)"
                        fillOpacity={0.12}
                    />
                </AreaChart>
            </ResponsiveContainer>
        </div>
    )
}
