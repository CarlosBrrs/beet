"use client"

import { useState } from "react"
import { InventoryStockResponse } from "@/lib/api-types"
import { useTransactions } from "@/lib/hooks/use-inventory"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { SheetShell } from "@/components/shared/sheet-shell"
import { Badge } from "@/components/ui/badge"
import { Loader2, ArrowUp, ArrowDown } from "lucide-react"
import { Button } from "@/components/ui/button"

const REASON_LABELS: Record<string, { label: string; variant: "default" | "destructive" | "outline" | "secondary" }> = {
    INITIAL: { label: "Initial", variant: "default" },
    PURCHASE: { label: "Purchase", variant: "default" },
    ADJUSTMENT: { label: "Adjustment", variant: "secondary" },
    WASTE: { label: "Waste", variant: "destructive" },
    CORRECTION: { label: "Correction", variant: "outline" },
    SALE: { label: "Sale", variant: "secondary" },
}

interface TransactionHistorySheetProps {
    stock: InventoryStockResponse | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function TransactionHistorySheet({ stock, open, onOpenChange }: TransactionHistorySheetProps) {
    const { restaurantId } = useRestaurantContext()
    const [page, setPage] = useState(0)

    const { data: pageResult, isLoading } = useTransactions({
        restaurantId: restaurantId!,
        stockId: stock?.id || "",
        page,
        size: 15,
    })

    const transactions = pageResult?.content || []

    if (!stock) return null

    return (
        <SheetShell
            open={open}
            onOpenChange={onOpenChange}
            title={`History: ${stock.ingredientName}`}
            description={`Transaction log for ${stock.ingredientName} (${stock.unitAbbreviation})`}
        >
            <div className="space-y-3">
                {isLoading ? (
                    <div className="flex items-center justify-center p-8">
                        <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                    </div>
                ) : transactions.length === 0 ? (
                    <div className="text-center text-sm text-muted-foreground p-8">
                        No transactions recorded yet.
                    </div>
                ) : (
                    <>
                        {transactions.map((tx) => {
                            const isPositive = tx.delta > 0
                            const reasonInfo = REASON_LABELS[tx.reason] || { label: tx.reason, variant: "outline" as const }
                            const date = new Date(tx.createdAt)

                            return (
                                <div
                                    key={tx.id}
                                    className="flex items-start gap-3 p-3 border rounded-lg"
                                >
                                    {/* Delta indicator */}
                                    <div className={`flex items-center justify-center h-8 w-8 rounded-full shrink-0 ${isPositive ? "bg-green-100 text-green-600" : "bg-red-100 text-red-600"
                                        }`}>
                                        {isPositive ? <ArrowUp className="h-4 w-4" /> : <ArrowDown className="h-4 w-4" />}
                                    </div>

                                    {/* Details */}
                                    <div className="flex-1 min-w-0 space-y-1">
                                        <div className="flex items-center justify-between">
                                            <span className={`font-bold text-sm ${isPositive ? "text-green-600" : "text-red-600"}`}>
                                                {isPositive ? "+" : ""}{tx.delta} {stock.unitAbbreviation}
                                            </span>
                                            <Badge variant={reasonInfo.variant} className="text-xs">
                                                {reasonInfo.label}
                                            </Badge>
                                        </div>

                                        <div className="flex items-center gap-2 text-xs text-muted-foreground">
                                            <span>{tx.previousStock} → {tx.resultingStock} {stock.unitAbbreviation}</span>
                                        </div>

                                        {tx.notes && (
                                            <p className="text-xs text-muted-foreground italic">
                                                {tx.notes}
                                            </p>
                                        )}

                                        <p className="text-xs text-muted-foreground">
                                            {date.toLocaleDateString()} {date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                                        </p>
                                    </div>
                                </div>
                            )
                        })}

                        {/* Pagination */}
                        {pageResult && pageResult.totalPages > 1 && (
                            <div className="flex items-center justify-between pt-2">
                                <Button
                                    variant="outline" size="sm"
                                    onClick={() => setPage(p => Math.max(0, p - 1))}
                                    disabled={pageResult.first}
                                >
                                    Previous
                                </Button>
                                <span className="text-xs text-muted-foreground">
                                    Page {pageResult.number + 1} of {pageResult.totalPages}
                                </span>
                                <Button
                                    variant="outline" size="sm"
                                    onClick={() => setPage(p => p + 1)}
                                    disabled={pageResult.last}
                                >
                                    Next
                                </Button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </SheetShell>
    )
}
