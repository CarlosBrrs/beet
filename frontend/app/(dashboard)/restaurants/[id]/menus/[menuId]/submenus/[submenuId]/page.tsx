"use client"

import { useParams, useRouter } from "next/navigation"
import { useMenus } from "@/lib/hooks/use-menus"
import { Button } from "@/components/ui/button"
import { ArrowLeft, Loader2 } from "lucide-react"
import { SubmenuNodeList } from "@/components/modules/menus/submenu-node-list"
import { SubmenuProductSheet } from "@/components/modules/products/submenu-product-sheet"
import { SubmenuTemplateSheet } from "@/components/modules/templates/submenu-template-sheet"
import { PublishCatalogNodeSheet } from "@/components/modules/menus/publish-catalog-node-sheet"
import { useState } from "react"

export default function SubmenuDetailPage() {
    const params = useParams()
    const router = useRouter()

    const menuId = params.menuId as string
    const submenuId = params.submenuId as string

    // Fetch all menus to get submenu details
    const { data: menus, isLoading: menuLoading } = useMenus()

    // Find the active menu and submenu
    const menu = menus?.find((m) => m.id === menuId)
    const submenu = menu?.submenus.find((s) => s.id === submenuId)

    // State for Sheets (Product / Template Modals)
    const [isProductSheetOpen, setIsProductSheetOpen] = useState(false)
    const [isTemplateSheetOpen, setIsTemplateSheetOpen] = useState(false)
    const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
    const [selectedNodeType, setSelectedNodeType] = useState<"PRODUCT" | "TEMPLATE" | null>(null)
    const [sheetMode, setSheetMode] = useState<"create" | "view" | "edit">("create")
    const [publishType, setPublishType] = useState<"PRODUCT" | "TEMPLATE" | null>(null)
    const [isPublishSheetOpen, setIsPublishSheetOpen] = useState(false)

    const handleAddProduct = () => {
        setSelectedNodeId(null)
        setSelectedNodeType("PRODUCT")
        setSheetMode("create")
        setIsProductSheetOpen(true)
    }

    const handleAddTemplate = () => {
        setSelectedNodeId(null)
        setSelectedNodeType("TEMPLATE")
        setSheetMode("create")
        setIsTemplateSheetOpen(true)
    }

    const handleViewNode = (nodeId: string, type: "PRODUCT" | "TEMPLATE") => {
        setSelectedNodeId(nodeId)
        setSelectedNodeType(type)
        setSheetMode("view")
        if (type === "PRODUCT") {
            setIsProductSheetOpen(true)
        } else {
            // Templates detail view not yet separated, but placeholder logic here
            setIsTemplateSheetOpen(true)
        }
    }

    const handleEditNode = (nodeId: string, type: "PRODUCT" | "TEMPLATE") => {
        setSelectedNodeId(nodeId)
        setSelectedNodeType(type)
        setSheetMode("edit")
        if (type === "PRODUCT") {
            setIsProductSheetOpen(true)
        } else {
            setIsTemplateSheetOpen(true)
        }
    }

    if (menuLoading) {
        return (
            <div className="flex justify-center p-12">
                <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
        )
    }

    if (!submenu) {
        return (
            <div className="flex flex-col items-center justify-center p-12 text-center text-muted-foreground space-y-4">
                <p>Submenú no encontrado.</p>
                <Button variant="outline" onClick={() => router.back()}>
                    Regresar
                </Button>
            </div>
        )
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col gap-4">
                <div className="flex items-center gap-4">
                    <Button variant="ghost" size="icon" onClick={() => router.back()} className="rounded-full">
                        <ArrowLeft className="h-5 w-5" />
                    </Button>
                    <div>
                        <h1 className="text-3xl font-bold tracking-tight">{submenu.name}</h1>
                        <p className="text-muted-foreground mt-1 text-sm">
                            Menú: <span className="font-medium text-foreground">{menu?.name}</span>
                        </p>
                    </div>
                </div>
                {submenu.description && (
                    <p className="text-muted-foreground pl-14 max-w-2xl">{submenu.description}</p>
                )}
            </div>

            {/* List */}
            <div className="pl-14">
                <SubmenuNodeList
                    menuId={menuId}
                    submenuId={submenuId}
                    onAddProduct={handleAddProduct}
                    onAddTemplate={handleAddTemplate}
                    onPublishProduct={() => { setPublishType("PRODUCT"); setIsPublishSheetOpen(true) }}
                    onPublishTemplate={() => { setPublishType("TEMPLATE"); setIsPublishSheetOpen(true) }}
                    onViewNode={handleViewNode}
                    onEditNode={handleEditNode}
                />
            </div>

            {/* Modals/Sheets */}
            <SubmenuProductSheet
                menuId={menuId}
                submenuId={submenuId}
                nodeId={selectedNodeType === "PRODUCT" ? selectedNodeId : null}
                mode={sheetMode}
                open={isProductSheetOpen}
                onOpenChange={setIsProductSheetOpen}
            />

            <SubmenuTemplateSheet
                menuId={menuId}
                submenuId={submenuId}
                nodeId={selectedNodeType === "TEMPLATE" ? selectedNodeId : null}
                open={isTemplateSheetOpen}
                onOpenChange={setIsTemplateSheetOpen}
            />
            <PublishCatalogNodeSheet
                menuId={menuId}
                submenuId={submenuId}
                nodeType={publishType}
                open={isPublishSheetOpen}
                onOpenChange={setIsPublishSheetOpen}
            />
        </div>
    )
}
