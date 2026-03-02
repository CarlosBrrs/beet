"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { PurchasesList } from "@/components/modules/purchases/purchases-list"
import { InvoiceDetailSheet } from "@/components/modules/purchases/invoice-detail-sheet"
import { Button } from "@/components/ui/button"
import { Can } from "@/components/shared/can"
import { Plus } from "lucide-react"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"

export default function PurchasesPage() {
    const router = useRouter()
    const { restaurantId } = useRestaurantContext()
    const [detailId, setDetailId] = useState<string | null>(null)

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-3xl font-bold">Purchases</h1>
                    <p className="text-muted-foreground">Register and track supplier invoices.</p>
                </div>
                <Can I="CREATE" a="INVOICES">
                    <Button onClick={() => router.push(`/restaurants/${restaurantId}/purchases/register`)}>
                        <Plus className="mr-2 h-4 w-4" />
                        Register Invoice
                    </Button>
                </Can>
            </div>

            <PurchasesList onViewDetail={(id) => setDetailId(id)} />

            <InvoiceDetailSheet
                invoiceId={detailId}
                open={!!detailId}
                onOpenChange={(open) => { if (!open) setDetailId(null) }}
            />
        </div>
    )
}
