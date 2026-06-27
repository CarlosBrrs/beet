"use client"

import { useInvoiceDetail } from "@/lib/hooks/use-invoices"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { SheetShell } from "@/components/shared/sheet-shell"
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"

interface InvoiceDetailSheetProps {
    invoiceId: string | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function InvoiceDetailSheet({ invoiceId, open, onOpenChange }: InvoiceDetailSheetProps) {
    const { restaurantId } = useRestaurantContext()
    const { data: detail, isLoading } = useInvoiceDetail(restaurantId!, invoiceId ?? "")

    const fmt = (n: number) =>
        new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 }).format(n)

    const fmtUnit = (n: number) =>
        new Intl.NumberFormat("es-CO", { maximumFractionDigits: 4 }).format(n)

    return (
        <SheetShell
            title="Invoice Detail"
            description="Detailed view of the purchase invoice."
            open={open}
            onOpenChange={onOpenChange}
            size="xl"
        >
            {isLoading ? (
                <p className="text-muted-foreground py-8 text-center">Loading...</p>
            ) : !detail ? (
                <p className="text-muted-foreground py-8 text-center">Invoice not found.</p>
            ) : (
                <div className="space-y-6 mt-4">
                    {/* Header */}
                    <div className="grid grid-cols-2 gap-4 text-sm">
                        <div>
                            <p className="text-muted-foreground">Supplier</p>
                            <p className="font-medium">{detail.supplierName}</p>
                        </div>
                        <div>
                            <p className="text-muted-foreground">Invoice #</p>
                            <p className="font-mono">{detail.supplierInvoiceNumber}</p>
                        </div>
                        <div>
                            <p className="text-muted-foreground">Date</p>
                            <p>{new Date(detail.emissionDate).toLocaleDateString("es-CO")}</p>
                        </div>
                        <div>
                            <p className="text-muted-foreground">Status</p>
                            <Badge variant={detail.status === "COMPLETED" ? "default" : "destructive"}>
                                {detail.status}
                            </Badge>
                        </div>
                        {detail.notes && (
                            <div className="col-span-2">
                                <p className="text-muted-foreground">Notes</p>
                                <p>{detail.notes}</p>
                            </div>
                        )}
                    </div>

                    <Separator />

                    {/* Items */}
                    <div>
                        <h3 className="font-semibold mb-3">Items</h3>
                        <div className="rounded-md border">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead>Ingredient</TableHead>
                                        <TableHead>Qty</TableHead>
                                        <TableHead className="text-right">Unit Price</TableHead>
                                        <TableHead className="text-right">Subtotal</TableHead>
                                        <TableHead className="text-right">Cost/base</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {detail.items.map((item) => (
                                        <TableRow key={item.id}>
                                            <TableCell>
                                                <p className="font-medium">{item.ingredientName}</p>
                                                <p className="text-xs text-muted-foreground">
                                                    {item.purchaseUnitName} (= {fmtUnit(item.conversionFactorUsed)} {item.baseUnitAbbreviation})
                                                </p>
                                            </TableCell>
                                            <TableCell>{fmtUnit(item.quantityPurchased)}</TableCell>
                                            <TableCell className="text-right">{fmt(item.unitPricePurchased)}</TableCell>
                                            <TableCell className="text-right">{fmt(item.subtotal)}</TableCell>
                                            <TableCell className="text-right font-mono text-xs">
                                                {fmt(item.costPerBaseUnit)}/{item.baseUnitAbbreviation}
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </div>
                    </div>

                    <Separator />

                    {/* Totals */}
                    <div className="space-y-2 text-sm">
                        <div className="flex justify-between">
                            <span className="text-muted-foreground">Subtotal</span>
                            <span>{fmt(detail.subtotal)}</span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-muted-foreground">Tax</span>
                            <span>{fmt(detail.totalTax)}</span>
                        </div>
                        <div className="flex justify-between text-base font-bold">
                            <span>Total</span>
                            <span>{fmt(detail.totalAmount)}</span>
                        </div>
                    </div>
                </div>
            )}
        </SheetShell>
    )
}
