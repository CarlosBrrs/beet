"use client"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { useMyPermissions } from "@/lib/hooks/use-my-permissions"
import { Skeleton } from "@/components/ui/skeleton"
import { CatalogRoadmapNote } from "@/components/modules/catalog/catalog-roadmap-note"
import { OperationsRoadmapNote } from "@/components/modules/orders/operations-roadmap-note"
import { DashboardReportSummary } from "@/components/modules/reports/dashboard-report-summary"
import { Can } from "@/components/shared/can"

export default function RestaurantDashboard() {
    const { restaurantId } = useRestaurantContext()
    const { data: permissions, isLoading } = useMyPermissions()

    // Find this restaurant's role, or fall back to OWNER if global permission exists
    const roleEntry = permissions?.find(p => p.restaurantId === restaurantId)
        ?? permissions?.find(p => p.restaurantId === null)
    const roleName = roleEntry?.role ?? null

    if (isLoading) {
        return (
            <div className="grid gap-4 md:grid-cols-3">
                <Skeleton className="h-32 rounded-xl" />
                <Skeleton className="h-32 rounded-xl" />
                <Skeleton className="h-32 rounded-xl" />
            </div>
        )
    }

    return (
        <div className="space-y-6">
            <h1 className="text-3xl font-bold">Dashboard</h1>
            <p className="text-muted-foreground">
                Welcome back! You are logged in as <span className="font-bold text-foreground">{roleName}</span>.
            </p>

            <Can I="VIEW" a="FINANCE">
                <DashboardReportSummary />
            </Can>

            <div className="border border-amber-300 bg-amber-50 p-3 text-xs text-amber-900">
                Pendiente no bloqueante: permitir editar la zona horaria del restaurante despues de crearlo. Afecta numeracion diaria de ordenes, cajas, cortes y reportes.
            </div>

            <OperationsRoadmapNote />
            <CatalogRoadmapNote />
        </div>
    )
}
