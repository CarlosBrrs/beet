"use client"

import { CashRegisterList } from "@/components/modules/cash-registers/cash-register-list"
import { RestaurantCashSessionManagement } from "@/components/modules/cash-registers/cash-session-management"
import { CashSessionPanel } from "@/components/modules/cash-registers/cash-session-panel"

export default function CashRegistersPage() {
    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-3xl font-bold tracking-tight">Cash Registers</h1>
            </div>
            <CashSessionPanel />
            <section className="space-y-4">
                <h2 className="text-lg font-semibold">Registers</h2>
                <CashRegisterList />
            </section>
            <RestaurantCashSessionManagement />
        </div>
    )
}
