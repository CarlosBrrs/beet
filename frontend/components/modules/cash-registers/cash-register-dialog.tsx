"use client"

import { DialogShell } from "@/components/shared/dialog-shell"
import { CashRegisterResponse } from "@/lib/api-types"

import { CashRegisterForm } from "./cash-register-form"

interface CashRegisterDialogProps {
    register?: CashRegisterResponse | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function CashRegisterDialog({ register, open, onOpenChange }: CashRegisterDialogProps) {
    return (
        <DialogShell
            open={open}
            onOpenChange={onOpenChange}
            title={register ? "Edit cash register" : "New cash register"}
        >
            <CashRegisterForm
                initialData={register}
                onSuccess={() => onOpenChange(false)}
            />
        </DialogShell>
    )
}
