"use client"

import { useState } from "react"
import { Box, Info, Layers, Search } from "lucide-react"
import { SheetShell } from "@/components/shared/sheet-shell"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { useProducts } from "@/lib/hooks/use-items"
import { useTemplates } from "@/lib/hooks/use-templates"
import { usePublishSubmenuNode } from "@/lib/hooks/use-submenu-nodes"
import { SubmenuNodeType } from "@/lib/api-types"
import { formatCurrency } from "@/lib/formatters"

interface PublishCatalogNodeSheetProps {
    menuId: string
    submenuId: string
    nodeType: SubmenuNodeType | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function PublishCatalogNodeSheet({ menuId, submenuId, nodeType, open, onOpenChange }: PublishCatalogNodeSheetProps) {
    const [search, setSearch] = useState("")
    const { data: productData } = useProducts({ size: 200 })
    const { data: templateData } = useTemplates({ size: 200 })
    const templates = templateData?.content ?? []
    const publish = usePublishSubmenuNode(menuId, submenuId)
    const normalizedSearch = search.trim().toLocaleLowerCase()
    const products = (productData?.content ?? []).filter((product) =>
        product.name.toLocaleLowerCase().includes(normalizedSearch)
    )
    const availableTemplates = templates.filter((template) =>
        template.name.toLocaleLowerCase().includes(normalizedSearch)
    )

    const getProductBlockReason = (product: (typeof products)[number]) => {
        if (!product.isActive) return "Producto inactivo"
        if (product.isPublished) return "Ya esta publicado en otro submenu"
        if (!product.salePrice || product.salePrice <= 0) return "Falta precio de venta"
        return null
    }

    const getTemplateBlockReason = (template: (typeof availableTemplates)[number]) => {
        if (!template.isActive) return "Armable inactivo"
        if (template.isPublished) return "Ya esta publicado en otro submenu"
        return null
    }

    const publishEntry = (referenceId: string) => {
        if (!nodeType) return
        publish.mutate({ nodeType, referenceId }, { onSuccess: () => onOpenChange(false) })
    }

    return (
        <SheetShell open={open} onOpenChange={onOpenChange} size="lg" title={`Publicar ${nodeType === "PRODUCT" ? "producto" : "armable"} existente`} description="Selecciona un elemento activo del catalogo. Se mostrara individualmente en este submenu.">
            <div className="mt-6 space-y-4">
                <div className="flex gap-3 border bg-muted/30 p-3 text-sm text-muted-foreground">
                    <Info className="mt-0.5 h-4 w-4 shrink-0" />
                    <p>
                        {nodeType === "PRODUCT"
                            ? "Para publicar un producto individualmente debe estar activo, sin otra publicacion y tener precio de venta. Los productos no disponibles se muestran con la razon."
                            : "Para publicar un armable debe estar activo y sin otra publicacion. Los armables no disponibles se muestran con la razon."}
                    </p>
                </div>
                <div className="relative">
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input className="pl-9" placeholder="Buscar..." value={search} onChange={(event) => setSearch(event.target.value)} />
                </div>
                <div className="divide-y border">
                    {nodeType === "PRODUCT" ? products.map((product) => {
                        const blockReason = getProductBlockReason(product)
                        return (
                            <div key={product.id} className="flex items-center justify-between gap-3 p-3">
                                <div className="flex min-w-0 items-center gap-3">
                                    <Box className="h-4 w-4 shrink-0 text-emerald-600" />
                                    <div>
                                        <p className="truncate text-sm font-medium">{product.name}</p>
                                        <p className="text-xs text-muted-foreground">{product.salePrice ? formatCurrency(product.salePrice) : "Sin precio de venta"}</p>
                                    </div>
                                </div>
                                <div className="flex items-center gap-2">
                                    {blockReason ? <Badge variant="secondary">{blockReason}</Badge> : <Badge variant="outline">Disponible</Badge>}
                                    <Button size="sm" disabled={publish.isPending || !!blockReason} onClick={() => publishEntry(product.id)}>Publicar</Button>
                                </div>
                            </div>
                        )
                    }) : availableTemplates.map((template) => {
                        const blockReason = getTemplateBlockReason(template)
                        return (
                            <div key={template.id} className="flex items-center justify-between gap-3 p-3">
                                <div className="flex min-w-0 items-center gap-3">
                                    <Layers className="h-4 w-4 shrink-0 text-indigo-600" />
                                    <div><p className="truncate text-sm font-medium">{template.name}</p><p className="text-xs text-muted-foreground">{formatCurrency(template.basePrice)}</p></div>
                                </div>
                                <div className="flex items-center gap-2">
                                    {blockReason ? <Badge variant="secondary">{blockReason}</Badge> : <Badge variant="outline">Disponible</Badge>}
                                    <Button size="sm" disabled={publish.isPending || !!blockReason} onClick={() => publishEntry(template.id)}>Publicar</Button>
                                </div>
                            </div>
                        )
                    })}
                    {(nodeType === "PRODUCT" ? products : availableTemplates).length === 0 && <p className="p-6 text-center text-sm text-muted-foreground">No hay elementos disponibles para publicar.</p>}
                </div>
            </div>
        </SheetShell>
    )
}
