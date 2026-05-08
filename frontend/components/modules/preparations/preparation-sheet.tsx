"use client"

import { ItemResponse } from "@/lib/api-types";
import { SheetShell } from "@/components/shared/sheet-shell";
import { PreparationForm } from "./preparation-form";

interface PreparationSheetProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    preparation?: ItemResponse;
}

export function PreparationSheet({ open, onOpenChange, preparation }: PreparationSheetProps) {
    return (
        <SheetShell
            title={preparation ? "Editar Preparación" : "Nueva Preparación"}
            description={preparation
                ? "Modifica los datos de esta preparación interna."
                : "Define el nombre, rendimiento y lista de ingredientes."}
            open={open}
            onOpenChange={onOpenChange}
            size="xl"
        >
            <div className="mt-4">
                <PreparationForm
                    initialData={preparation}
                    onSuccess={() => onOpenChange(false)}
                />
            </div>
        </SheetShell>
    );
}
