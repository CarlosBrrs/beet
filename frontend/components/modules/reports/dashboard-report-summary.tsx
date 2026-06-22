"use client"

import Link from "next/link"
import { ArrowRight, CircleDollarSign, ReceiptText, ShoppingBag } from "lucide-react"
import type { ComponentType } from "react"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { Button } from "@/components/ui/button"
import { formatCurrency } from "@/lib/formatters"
import { useReportOverview } from "@/lib/hooks/use-reports"

function localIso(date: Date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, "0")
    const day = String(date.getDate()).padStart(2, "0")
    return `${year}-${month}-${day}`
}

export function DashboardReportSummary() {
    const { restaurantId } = useRestaurantContext()
    const today = localIso(new Date())
    const { data, isLoading } = useReportOverview({ dateFrom: today, dateTo: today })

    if (isLoading) {
        return <div className="h-28 animate-pulse border bg-muted/30" />
    }

    return (
        <section>
            <div className="mb-3 flex items-center justify-between">
                <div>
                    <h2 className="text-sm font-semibold">Operacion de hoy</h2>
                    <p className="text-xs text-muted-foreground">
                        {data?.provisional ? "Datos provisionales del dia abierto." : "Datos del dia operativo."}
                    </p>
                </div>
                <Button variant="ghost" size="sm" asChild>
                    <Link href={`/restaurants/${restaurantId}/reports`}>
                        Ver reportes
                        <ArrowRight className="h-4 w-4" />
                    </Link>
                </Button>
            </div>
            <div className="grid border md:grid-cols-3">
                <DashboardMetric
                    icon={ReceiptText}
                    label="Ventas brutas"
                    value={formatCurrency(data?.grossSales ?? 0)}
                />
                <DashboardMetric
                    icon={CircleDollarSign}
                    label="Cobrado neto"
                    value={formatCurrency(data?.netCollected ?? 0)}
                />
                <DashboardMetric
                    icon={ShoppingBag}
                    label="Ordenes completadas"
                    value={String(data?.completedOrders ?? 0)}
                />
            </div>
        </section>
    )
}

function DashboardMetric({
    icon: Icon,
    label,
    value,
}: {
    icon: ComponentType<{ className?: string }>
    label: string
    value: string
}) {
    return (
        <div className="flex items-center gap-3 border-b p-4 md:border-b-0 md:border-r">
            <div className="flex h-9 w-9 items-center justify-center border bg-muted/40">
                <Icon className="h-4 w-4" />
            </div>
            <div>
                <p className="text-xs text-muted-foreground">{label}</p>
                <p className="font-semibold">{value}</p>
            </div>
        </div>
    )
}
