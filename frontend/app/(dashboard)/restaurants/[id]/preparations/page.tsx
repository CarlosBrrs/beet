"use client"

import { useState } from "react"
import { PreparationList } from "@/components/modules/preparations/preparation-list"
import { PreparationSheet } from "@/components/modules/preparations/preparation-sheet"
import { Button } from "@/components/ui/button"
import { Can } from "@/components/shared/can"
import { Plus } from "lucide-react"
import { ItemResponse } from "@/lib/api-types"

export default function PreparationsPage() {
    const [selectedItem, setSelectedItem] = useState<ItemResponse | undefined>(undefined)
    const [isSheetOpen, setIsSheetOpen] = useState(false)

    const handleNew = () => {
        setSelectedItem(undefined)
        setIsSheetOpen(true)
    }

    const handleEdit = (item: ItemResponse) => {
        setSelectedItem(item)
        setIsSheetOpen(true)
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Preparaciones</h1>
                    <p className="text-muted-foreground mt-1">
                        Sub-recetas internas: salsas, mezclas, bases de masa, etc.
                    </p>
                </div>
                <Can I="CREATE" a="PREPARATIONS">
                    <Button onClick={handleNew} className="w-full sm:w-auto">
                        <Plus className="mr-2 h-4 w-4" />
                        Nueva Preparación
                    </Button>
                </Can>
            </div>

            <Can I="VIEW" a="PREPARATIONS">
                <PreparationList onNew={handleNew} onEdit={handleEdit} />
            </Can>

            <PreparationSheet
                open={isSheetOpen}
                onOpenChange={setIsSheetOpen}
                preparation={selectedItem}
            />
        </div>
    )
}
