"use client"

import { useState } from "react"
import { MenuList } from "@/components/modules/menus/menu-list"
import { MenuDetailSheet } from "@/components/modules/menus/menu-detail-sheet"
import { Button } from "@/components/ui/button"
import { Can } from "@/components/shared/can"
import { Plus } from "lucide-react"

export default function MenusPage() {
    const [selectedMenuId, setSelectedMenuId] = useState<string | null>(null)
    const [isSheetOpen, setIsSheetOpen] = useState(false)

    const handleCreateMenu = () => {
        setSelectedMenuId(null)
        setIsSheetOpen(true)
    }

    const handleEditMenu = (id: string) => {
        setSelectedMenuId(id)
        setIsSheetOpen(true)
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Menus</h1>
                    <p className="text-muted-foreground mt-1">
                        Manage your restaurant menus, categories, and structure.
                    </p>
                </div>
                <Can I="CREATE" a="MENUS">
                    <Button onClick={handleCreateMenu} className="w-full sm:w-auto">
                        <Plus className="mr-2 h-4 w-4" />
                        New Menu
                    </Button>
                </Can>
            </div>

            <Can I="VIEW" a="MENUS">
                <MenuList onEditMenu={handleEditMenu} />
            </Can>

            <MenuDetailSheet
                menuId={selectedMenuId}
                open={isSheetOpen}
                onOpenChange={setIsSheetOpen}
            />
        </div>
    )
}
