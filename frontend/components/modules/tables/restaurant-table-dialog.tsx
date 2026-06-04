"use client"

import { DialogShell } from "@/components/shared/dialog-shell"
import { RestaurantTableResponse } from "@/lib/api-types"

import { RestaurantTableForm } from "./restaurant-table-form"

interface RestaurantTableDialogProps {
    table?: RestaurantTableResponse | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function RestaurantTableDialog({ table, open, onOpenChange }: RestaurantTableDialogProps) {
    return (
        <DialogShell
            open={open}
            onOpenChange={onOpenChange}
            title={table ? "Edit table" : "New table"}
        >
            <RestaurantTableForm initialData={table} onSuccess={() => onOpenChange(false)} />
        </DialogShell>
    )
}
