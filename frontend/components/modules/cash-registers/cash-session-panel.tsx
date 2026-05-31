"use client"

import { Clock3, LogIn, LogOut, MonitorSmartphone, WalletCards } from "lucide-react"
import { useState } from "react"

import { Can } from "@/components/shared/can"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { formatCurrency } from "@/lib/formatters"
import { useCashRegisters } from "@/lib/hooks/use-cash-registers"
import { useActiveCashSession } from "@/lib/hooks/use-cash-sessions"

import { CloseCashSessionDialog } from "./close-cash-session-dialog"
import { OpenCashSessionDialog } from "./open-cash-session-dialog"

function formatOpenedAt(value: string) {
    return new Intl.DateTimeFormat("es-CO", {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(new Date(value))
}

function shortId(value: string) {
    return `${value.slice(0, 8)}...`
}

export function CashSessionPanel() {
    const { data: session, isLoading, isError, error } = useActiveCashSession()
    const { data: registers = [] } = useCashRegisters()
    const [openDialogOpen, setOpenDialogOpen] = useState(false)
    const [closeDialogOpen, setCloseDialogOpen] = useState(false)
    const register = registers.find((item) => item.id === session?.cashRegisterId)

    if (isLoading) {
        return (
            <section className="border">
                <div className="border-b px-4 py-3">
                    <Skeleton className="h-5 w-40" />
                </div>
                <div className="grid gap-3 p-4 sm:grid-cols-3">
                    <Skeleton className="h-12" />
                    <Skeleton className="h-12" />
                    <Skeleton className="h-12" />
                </div>
            </section>
        )
    }

    if (isError) {
        return (
            <section className="border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
                {error.message || "Failed to load cash session"}
            </section>
        )
    }

    if (!session) {
        return (
            <>
                <section className="flex flex-col gap-4 border p-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-center gap-3">
                        <div className="flex h-9 w-9 items-center justify-center border bg-muted/40">
                            <WalletCards className="h-4 w-4 text-muted-foreground" />
                        </div>
                        <div>
                            <h2 className="text-sm font-semibold">Current device session</h2>
                            <p className="text-xs text-muted-foreground">No open cash session</p>
                        </div>
                    </div>
                    <Can I="OPEN" a="CASH">
                        <Button onClick={() => setOpenDialogOpen(true)}>
                            <LogIn className="h-4 w-4" />
                            Open session
                        </Button>
                    </Can>
                </section>

                <OpenCashSessionDialog
                    open={openDialogOpen}
                    onOpenChange={setOpenDialogOpen}
                />
            </>
        )
    }

    return (
        <>
            <section className="border">
                <div className="flex flex-col gap-3 border-b px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-center gap-2">
                        <h2 className="text-sm font-semibold">Current device session</h2>
                        <Badge variant="outline" className="border-green-300 bg-green-50 text-green-700">
                            Open
                        </Badge>
                    </div>
                    <Can I="CLOSE" a="CASH">
                        <Button variant="outline" onClick={() => setCloseDialogOpen(true)}>
                            <LogOut className="h-4 w-4" />
                            Close session
                        </Button>
                    </Can>
                </div>
                <div className="grid gap-px bg-border sm:grid-cols-3">
                    <div className="bg-background p-4">
                        <p className="mb-1 flex items-center gap-1.5 text-xs text-muted-foreground">
                            <WalletCards className="h-3.5 w-3.5" />
                            Cash register
                        </p>
                        <p className="text-sm font-medium">{register?.name ?? shortId(session.cashRegisterId)}</p>
                    </div>
                    <div className="bg-background p-4">
                        <p className="mb-1 flex items-center gap-1.5 text-xs text-muted-foreground">
                            <Clock3 className="h-3.5 w-3.5" />
                            Opened at
                        </p>
                        <p className="text-sm font-medium">{formatOpenedAt(session.openedAt)}</p>
                    </div>
                    <div className="bg-background p-4">
                        <p className="mb-1 flex items-center gap-1.5 text-xs text-muted-foreground">
                            <MonitorSmartphone className="h-3.5 w-3.5" />
                            Opening amount
                        </p>
                        <p className="text-sm font-medium">{formatCurrency(Number(session.openingAmount))}</p>
                    </div>
                </div>
            </section>

            <CloseCashSessionDialog
                session={session}
                open={closeDialogOpen}
                onOpenChange={setCloseDialogOpen}
            />
        </>
    )
}
