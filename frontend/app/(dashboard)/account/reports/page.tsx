import { Suspense } from "react"

import { ReportsWorkspace } from "@/components/modules/reports/reports-workspace"

export default function AccountReportsPage() {
    return (
        <Suspense fallback={<div className="h-64 animate-pulse border bg-muted/30" />}>
            <ReportsWorkspace />
        </Suspense>
    )
}
