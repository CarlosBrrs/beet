"use client"

import { RestaurantTableGrid } from "@/components/modules/tables/restaurant-table-grid"

export default function TablesPage() {
    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-3xl font-bold">Tables</h1>
                <p className="mt-1 text-muted-foreground">Manage dine-in tables and review current availability.</p>
            </div>
            <RestaurantTableGrid />
        </div>
    )
}
