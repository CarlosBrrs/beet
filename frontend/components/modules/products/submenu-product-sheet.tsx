"use client"

import { SheetShell } from "@/components/shared/sheet-shell"
import { ProductForm } from "./product-form"
import { ProductDetail } from "./product-detail"

interface SubmenuProductSheetProps {
    menuId: string
    submenuId: string
    nodeId: string | null
    mode: "create" | "view" | "edit"
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function SubmenuProductSheet({
    menuId,
    submenuId,
    nodeId,
    mode,
    open,
    onOpenChange,
}: SubmenuProductSheetProps) {
    const isEditing = mode === "edit"
    const isViewing = mode === "view"
    // If editing, we would fetch the product here using `useProduct` hook from `use-submenu-nodes` or similar
    // For now, the creation flow is prioritized

    return (
        <SheetShell
            open={open}
            onOpenChange={onOpenChange}
            size="xl"
            title={isViewing ? "Detalles del Producto" : isEditing ? "Editar Producto" : "Nuevo Producto"}
            description={isViewing 
                ? "Consulta los parámetros y la receta del producto."
                : isEditing
                    ? "Modifica los detalles del producto o su receta asociada."
                    : "Agrega un nuevo producto a este submenú. Puede ser un ítem plano o preparado (receta)."
            }
        >
            <div className="mt-6">
                {isViewing && nodeId ? (
                    <ProductDetail 
                        menuId={menuId}
                        submenuId={submenuId}
                        productId={nodeId}
                    />
                ) : (
                    <ProductForm
                        menuId={menuId}
                        submenuId={submenuId}
                        productId={nodeId}
                        onSuccess={() => onOpenChange(false)}
                    />
                )}
            </div>
        </SheetShell>
    )
}
