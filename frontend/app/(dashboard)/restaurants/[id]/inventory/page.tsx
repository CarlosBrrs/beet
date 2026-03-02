"use client"

import { useState } from "react"
import { InventoryStockResponse } from "@/lib/api-types"
import { InventoryList } from "@/components/modules/inventory/inventory-list"
import { ActivateIngredientDialog } from "@/components/modules/inventory/activate-ingredient-dialog"
import { StockAdjustmentDialog } from "@/components/modules/inventory/stock-adjustment-dialog"
import { TransactionHistorySheet } from "@/components/modules/inventory/transaction-history-sheet"
import { Button } from "@/components/ui/button"
import { Can } from "@/components/shared/can"
import { Plus } from "lucide-react"

// TODO: Add option to deactivate/remove ingredient from this restaurant's inventory.
// Need to handle: stock > 0, ingredient used in recipes/products. Backend endpoint TBD.

export default function InventoryPage() {
    const [activateOpen, setActivateOpen] = useState(false)
    const [adjustStock, setAdjustStock] = useState<InventoryStockResponse | null>(null)
    const [historyStock, setHistoryStock] = useState<InventoryStockResponse | null>(null)

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold">Inventory</h1>
                    <p className="text-muted-foreground">Manage your restaurant&apos;s ingredient stock levels.</p>
                </div>
                <Can I="ACTIVATE" a="INVENTORY">
                    <Button onClick={() => setActivateOpen(true)}>
                        <Plus className="mr-2 h-4 w-4" />
                        Add from Catalog
                    </Button>
                </Can>
            </div>

            <InventoryList
                onAdjust={(stock) => setAdjustStock(stock)}
                onHistory={(stock) => setHistoryStock(stock)}
            />

            <ActivateIngredientDialog
                open={activateOpen}
                onOpenChange={setActivateOpen}
            />

            <StockAdjustmentDialog
                stock={adjustStock}
                open={!!adjustStock}
                onOpenChange={(open) => { if (!open) setAdjustStock(null) }}
            />

            <TransactionHistorySheet
                stock={historyStock}
                open={!!historyStock}
                onOpenChange={(open) => { if (!open) setHistoryStock(null) }}
            />
        </div>
    )
}
