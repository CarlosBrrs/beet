"use client"

import { useState, useCallback, useMemo } from "react"
import { useRouter } from "next/navigation"
import { useSuppliers } from "@/lib/hooks/use-ingredients"
import { useSupplierItems, useRegisterInvoice } from "@/lib/hooks/use-invoices"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { useMyRestaurants } from "@/lib/hooks/use-my-restaurants"
import { RegisterInvoiceRequest, SupplierItemForInvoiceResponse } from "@/lib/api-types"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Plus, Trash2, ArrowLeft, Calculator } from "lucide-react"
import { toast } from "sonner"

interface InvoiceLineItem {
    key: string
    supplierItemId: string
    ingredientName: string
    purchaseUnitName: string
    baseUnitAbbreviation: string
    conversionFactor: number
    quantityPurchased: string
    unitPricePurchased: string // Raw numeric string
    taxPercentage: string
    lastCostBase: number | null
}

export function RegisterInvoiceForm() {
    const router = useRouter()
    const { restaurantId } = useRestaurantContext()
    const { data: suppliers } = useSuppliers()
    const { data: myRestaurants } = useMyRestaurants()
    const registerMutation = useRegisterInvoice(restaurantId!)

    const currentRestaurant = myRestaurants?.find(r => r.id === restaurantId)
    const settings = currentRestaurant?.settings
    const taxApplyMode = settings?.taxApplyMode ?? "PER_ITEM"
    const defaultTaxPercentage = settings?.defaultTaxPercentage ?? 19

    // Header state
    const [supplierId, setSupplierId] = useState<string>("")
    const [invoiceNumber, setInvoiceNumber] = useState("")
    const [emissionDate, setEmissionDate] = useState(new Date().toISOString().slice(0, 10))
    const [notes, setNotes] = useState("")
    const [globalTaxPercentage, setGlobalTaxPercentage] = useState<string>(String(defaultTaxPercentage))

    // Items
    const [items, setItems] = useState<InvoiceLineItem[]>([])

    // Supplier items for the selected supplier
    const { data: supplierItems, isLoading: loadingItems } = useSupplierItems(restaurantId!, supplierId || null)

    // ── Add Item ──
    const addItem = useCallback((si: SupplierItemForInvoiceResponse) => {
        setItems(prev => [...prev, {
            key: `${si.id}-${Date.now()}`,
            supplierItemId: si.id,
            ingredientName: si.ingredientName,
            purchaseUnitName: si.purchaseUnitName,
            baseUnitAbbreviation: si.baseUnitAbbreviation,
            conversionFactor: si.conversionFactor,
            quantityPurchased: "1",
            unitPricePurchased: si.lastCostBase
                ? String(Math.round(si.lastCostBase * si.conversionFactor))
                : "",
            taxPercentage: String(defaultTaxPercentage),
            lastCostBase: si.lastCostBase,
        }])
    }, [defaultTaxPercentage])

    // ── Update Item Field ──
    const updateItem = useCallback((key: string, field: keyof InvoiceLineItem, value: string) => {
        setItems(prev => prev.map(item =>
            item.key === key ? { ...item, [field]: value } : item
        ))
    }, [])

    // ── Remove Item ──
    const removeItem = useCallback((key: string) => {
        setItems(prev => prev.filter(item => item.key !== key))
    }, [])

    // ── Computed totals ──
    const { subtotal, totalTax, totalAmount, hasErrors } = useMemo(() => {
        let sub = 0; let tax = 0; let err = false
        const globalTax = parseFloat(globalTaxPercentage) || 0

        for (const item of items) {
            const qty = parseFloat(item.quantityPurchased) || 0
            const price = parseFloat(item.unitPricePurchased) || 0

            let taxPct = 0
            if (taxApplyMode === "PER_INVOICE") {
                taxPct = globalTax
            } else {
                taxPct = parseFloat(item.taxPercentage) || 0
            }

            if (qty <= 0 || price <= 0) err = true

            const lineSubtotal = qty * price
            const lineTax = lineSubtotal * taxPct / 100

            sub += lineSubtotal
            tax += lineTax
        }
        return { subtotal: sub, totalTax: tax, totalAmount: sub + tax, hasErrors: err }
    }, [items, taxApplyMode, globalTaxPercentage])

    // ── Format currency ──
    const fmt = useCallback((n: number) =>
        new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 }).format(n), [])

    // ── Price change detection ──
    const getPriceAlert = useCallback((item: InvoiceLineItem): string | null => {
        if (!item.lastCostBase) return null
        const currentUnitPrice = parseFloat(item.unitPricePurchased) || 0
        if (currentUnitPrice <= 0) return null
        const expectedPrice = item.lastCostBase * item.conversionFactor
        const percentageDiff = ((currentUnitPrice - expectedPrice) / expectedPrice) * 100

        if (Math.abs(percentageDiff) > 2) {
            const amountDiff = Math.abs(currentUnitPrice - expectedPrice)
            const baseAmountDiff = amountDiff / item.conversionFactor

            return percentageDiff > 0
                ? `⬆ Price increased ${percentageDiff.toFixed(1)}% (+${fmt(amountDiff)} / ${item.purchaseUnitName}, +${fmt(baseAmountDiff)} / ${item.baseUnitAbbreviation})`
                : `⬇ Price decreased ${Math.abs(percentageDiff).toFixed(1)}% (-${fmt(amountDiff)} / ${item.purchaseUnitName}, -${fmt(baseAmountDiff)} / ${item.baseUnitAbbreviation})`
        }
        return null
    }, [fmt])

    // ── Submit ──
    const canSubmit = !!supplierId && !!invoiceNumber && !!emissionDate && items.length > 0 && !hasErrors

    const handleSubmit = async () => {
        const globalTax = parseFloat(globalTaxPercentage) || 0

        const request: RegisterInvoiceRequest = {
            supplierId,
            supplierInvoiceNumber: invoiceNumber,
            emissionDate,
            notes: notes || undefined,
            items: items.map(item => ({
                supplierItemId: item.supplierItemId,
                quantityPurchased: parseFloat(item.quantityPurchased),
                unitPricePurchased: parseFloat(item.unitPricePurchased),
                // If PER_INVOICE, we send the global tax on every item to match backend requirements
                taxPercentage: taxApplyMode === "PER_INVOICE" ? globalTax : (parseFloat(item.taxPercentage) || 0),
                conversionFactorUsed: item.conversionFactor,
            })),
        }

        try {
            await registerMutation.mutateAsync(request)
            toast.success("Invoice registered successfully!")
            router.push(`/restaurants/${restaurantId}/purchases`)
        } catch {
            toast.error("Failed to register invoice. Please try again.")
        }
    }

    return (
        <div className="space-y-8 max-w-5xl">
            <Button variant="ghost" size="sm" onClick={() => router.back()}>
                <ArrowLeft className="mr-2 h-4 w-4" /> Back to Purchases
            </Button>

            <Card>
                <CardHeader>
                    <CardTitle>Invoice Header</CardTitle>
                </CardHeader>
                <CardContent className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    <div className="space-y-2">
                        <Label htmlFor="supplier">Supplier *</Label>
                        <Select value={supplierId} onValueChange={(v) => { setSupplierId(v); setItems([]) }}>
                            <SelectTrigger id="supplier">
                                <SelectValue placeholder="Select supplier..." />
                            </SelectTrigger>
                            <SelectContent>
                                {suppliers?.filter(s => s.isActive).map(s => (
                                    <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="invoiceNumber">Invoice Number *</Label>
                        <Input
                            id="invoiceNumber"
                            placeholder="e.g. FAC-001234"
                            value={invoiceNumber}
                            onChange={e => setInvoiceNumber(e.target.value)}
                        />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="emissionDate">Emission Date *</Label>
                        <Input
                            id="emissionDate"
                            type="date"
                            value={emissionDate}
                            onChange={e => setEmissionDate(e.target.value)}
                        />
                    </div>
                    {taxApplyMode === "PER_INVOICE" && (
                        <div className="space-y-2">
                            <Label htmlFor="globalTax">Global Tax % *</Label>
                            <Input
                                id="globalTax"
                                type="number"
                                min="0"
                                max="100"
                                step="0.01"
                                value={globalTaxPercentage}
                                onChange={e => setGlobalTaxPercentage(e.target.value)}
                            />
                        </div>
                    )}
                    <div className={`space-y-2 ${taxApplyMode === "PER_INVOICE" ? "lg:col-span-2" : "md:col-span-2 lg:col-span-3"}`}>
                        <Label htmlFor="notes">Notes</Label>
                        <Textarea
                            id="notes"
                            placeholder="Optional notes..."
                            value={notes}
                            onChange={e => setNotes(e.target.value)}
                            rows={1}
                        />
                    </div>
                </CardContent>
            </Card>

            {supplierId && (
                <Card>
                    <CardHeader className="flex flex-row items-center justify-between">
                        <CardTitle>Invoice Items</CardTitle>
                        <Select
                            value=""
                            onValueChange={(id) => {
                                const si = supplierItems?.find(s => s.id === id)
                                if (si) addItem(si)
                            }}
                        >
                            <SelectTrigger className="w-[280px]">
                                <SelectValue placeholder={loadingItems ? "Loading items..." : "Add item..."} />
                            </SelectTrigger>
                            <SelectContent>
                                {supplierItems?.map(si => (
                                    <SelectItem key={si.id} value={si.id}>
                                        {si.ingredientName} — {si.brandName} ({si.purchaseUnitName})
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </CardHeader>
                    <CardContent>
                        {items.length === 0 ? (
                            <p className="text-muted-foreground text-center py-8">
                                Select items from the dropdown above to add them to the invoice.
                            </p>
                        ) : (
                            <div className="space-y-4">
                                <div className="rounded-md border overflow-x-auto">
                                    <Table>
                                        <TableHeader>
                                            <TableRow>
                                                <TableHead className="min-w-[180px]">Ingredient ({items.length})</TableHead>
                                                <TableHead className="w-[120px]">Quantity</TableHead>
                                                <TableHead className="w-[150px]">Unit Price ($)</TableHead>
                                                {taxApplyMode === "PER_ITEM" && (
                                                    <TableHead className="w-[100px]">Tax %</TableHead>
                                                )}
                                                <TableHead className="text-right w-[120px]">Subtotal</TableHead>
                                                <TableHead className="w-[50px]"></TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {items.map(item => {
                                                const qty = parseFloat(item.quantityPurchased) || 0
                                                const price = parseFloat(item.unitPricePurchased) || 0
                                                const lineSubtotal = qty * price
                                                const priceAlert = getPriceAlert(item)
                                                const baseQty = qty * item.conversionFactor

                                                return (
                                                    <TableRow key={item.key}>
                                                        <TableCell>
                                                            <p className="font-medium">{item.ingredientName}</p>
                                                            <p className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                                                                <Calculator className="h-3 w-3" />
                                                                1 {item.purchaseUnitName} = {item.conversionFactor} {item.baseUnitAbbreviation}
                                                            </p>
                                                            {qty > 0 && (
                                                                <p className="text-xs text-muted-foreground mt-0.5 font-mono bg-muted/50 w-fit px-1.5 rounded">
                                                                    Total: {(baseQty).toFixed(2)} {item.baseUnitAbbreviation}
                                                                </p>
                                                            )}
                                                            {priceAlert && (
                                                                <p className={`text-xs mt-1 font-medium ${priceAlert.startsWith("⬆") ? "text-orange-500" : "text-green-500"}`}>
                                                                    {priceAlert}
                                                                </p>
                                                            )}
                                                        </TableCell>
                                                        <TableCell>
                                                            <Input
                                                                type="number"
                                                                min="0"
                                                                step="0.01"
                                                                value={item.quantityPurchased}
                                                                onChange={e => updateItem(item.key, "quantityPurchased", e.target.value)}
                                                                className="w-full"
                                                            />
                                                        </TableCell>
                                                        <TableCell>
                                                            <Input
                                                                type="text"
                                                                value={item.unitPricePurchased ? parseInt(item.unitPricePurchased).toLocaleString('es-CO') : ""}
                                                                onChange={e => updateItem(item.key, "unitPricePurchased", e.target.value.replace(/\D/g, ""))}
                                                                className="w-full font-mono text-right"
                                                            />
                                                        </TableCell>
                                                        {taxApplyMode === "PER_ITEM" && (
                                                            <TableCell>
                                                                <Input
                                                                    type="number"
                                                                    min="0"
                                                                    max="100"
                                                                    step="0.01"
                                                                    value={item.taxPercentage}
                                                                    onChange={e => updateItem(item.key, "taxPercentage", e.target.value)}
                                                                    className="w-full"
                                                                />
                                                            </TableCell>
                                                        )}
                                                        <TableCell className="text-right font-medium">
                                                            {fmt(lineSubtotal)}
                                                        </TableCell>
                                                        <TableCell>
                                                            <Button variant="ghost" size="icon" onClick={() => removeItem(item.key)}>
                                                                <Trash2 className="h-4 w-4 text-destructive" />
                                                            </Button>
                                                        </TableCell>
                                                    </TableRow>
                                                )
                                            })}
                                        </TableBody>
                                    </Table>
                                </div>

                                <Separator />
                                <div className="flex justify-end">
                                    <div className="w-64 space-y-2 text-sm">
                                        <div className="flex justify-between">
                                            <span className="text-muted-foreground">Subtotal</span>
                                            <span>{fmt(subtotal)}</span>
                                        </div>
                                        <div className="flex justify-between">
                                            <span className="text-muted-foreground">
                                                Tax {taxApplyMode === "PER_INVOICE" && `(${globalTaxPercentage}%)`}
                                            </span>
                                            <span>{fmt(totalTax)}</span>
                                        </div>
                                        <Separator />
                                        <div className="flex justify-between text-base font-bold">
                                            <span>Total</span>
                                            <span>{fmt(totalAmount)}</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </CardContent>
                </Card>
            )}

            <div className="flex justify-end gap-4">
                <Button variant="outline" onClick={() => router.back()}>
                    Cancel
                </Button>
                <Button
                    disabled={!canSubmit || registerMutation.isPending}
                    onClick={handleSubmit}
                >
                    {registerMutation.isPending ? "Registering..." : "Register Invoice"}
                </Button>
            </div>
        </div>
    )
}
