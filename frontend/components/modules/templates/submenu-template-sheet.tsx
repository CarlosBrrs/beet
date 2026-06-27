"use client"

import { SheetShell } from "@/components/shared/sheet-shell"
import { TemplateForm } from "./template-form"

interface SubmenuTemplateSheetProps {
    menuId: string
    submenuId: string
    nodeId: string | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function SubmenuTemplateSheet({
    menuId,
    submenuId,
    nodeId,
    open,
    onOpenChange,
}: SubmenuTemplateSheetProps) {
    const isEditing = !!nodeId
    // If editing, we would fetch the template here using node relation

    return (
        <SheetShell
            open={open}
            onOpenChange={onOpenChange}
            size="xl"
            title={isEditing ? "Editar Plantilla (Combo)" : "Nueva Plantilla (Combo)"}
            description={isEditing
                ? "Modifica los detalles del combo, sus grupos (slots) u opciones."
                : "Crea un nuevo Combo agregando grupos de opciones (ej. Bebidas, Acompañamientos) y eligiendo productos de tu inventario."
            }
        >
            <div className="mt-6">
                <TemplateForm
                    menuId={menuId}
                    submenuId={submenuId}
                    templateId={nodeId}
                    onSuccess={() => onOpenChange(false)}
                />
            </div>
        </SheetShell>
    )
}
