"use client"

import { RegisterInvoiceForm } from "@/components/modules/purchases/register-invoice-form"

export default function RegisterPurchasePage() {
    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-3xl font-bold">Register Invoice</h1>
                <p className="text-muted-foreground">Record a supplier invoice and update stock automatically.</p>
            </div>
            <RegisterInvoiceForm />
        </div>
    )
}
