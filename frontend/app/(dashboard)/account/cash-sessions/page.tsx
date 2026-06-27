"use client"

import { AccountCashSessionManagement } from "@/components/modules/cash-registers/cash-session-management"
import { Can } from "@/components/shared/can"

export default function AccountCashSessionsPage() {
    return (
        <div className="space-y-6">
            <h1 className="text-3xl font-bold tracking-tight">Cash Sessions</h1>
            <Can I="VIEW" a="CASH">
                <AccountCashSessionManagement />
            </Can>
        </div>
    )
}
