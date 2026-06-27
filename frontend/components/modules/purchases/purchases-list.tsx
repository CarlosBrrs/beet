"use client"

import { useState } from "react"
import { useInvoices, InvoiceListParams } from "@/lib/hooks/use-invoices"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { useDebounce } from "@/lib/hooks/use-debounce"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import { ChevronLeft, ChevronRight, Eye, Search } from "lucide-react"

interface PurchasesListProps {
    onViewDetail: (invoiceId: string) => void
}

export function PurchasesList({ onViewDetail }: PurchasesListProps) {
    const { restaurantId } = useRestaurantContext()
    const [page, setPage] = useState(0)
    const [searchInput, setSearchInput] = useState("")
    const debouncedSearch = useDebounce(searchInput, 300)

    const params: InvoiceListParams = {
        restaurantId: restaurantId!,
        page,
        size: 10,
        search: debouncedSearch || undefined,
    }

    const { data, isLoading } = useInvoices(params)

    const formatCurrency = (amount: number) =>
        new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 }).format(amount)

    const formatDate = (dateStr: string) =>
        new Date(dateStr).toLocaleDateString("es-CO", { year: "numeric", month: "short", day: "numeric" })

    return (
        <div className="space-y-4">
            {/* Search */}
            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                    placeholder="Search by supplier or invoice #..."
                    value={searchInput}
                    onChange={(e) => { setSearchInput(e.target.value); setPage(0) }}
                    className="pl-10"
                />
            </div>

            {/* Table */}
            <div className="rounded-md border">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Date</TableHead>
                            <TableHead>Supplier</TableHead>
                            <TableHead>Invoice #</TableHead>
                            <TableHead className="text-right">Total</TableHead>
                            <TableHead className="text-center">Items</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead className="w-[60px]"></TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {isLoading ? (
                            <TableRow>
                                <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                                    Loading...
                                </TableCell>
                            </TableRow>
                        ) : !data?.content?.length ? (
                            <TableRow>
                                <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                                    No invoices registered yet.
                                </TableCell>
                            </TableRow>
                        ) : (
                            data.content.map((invoice) => (
                                <TableRow key={invoice.id}>
                                    <TableCell>{formatDate(invoice.emissionDate)}</TableCell>
                                    <TableCell className="font-medium">{invoice.supplierName ?? "—"}</TableCell>
                                    <TableCell className="font-mono text-sm">{invoice.supplierInvoiceNumber}</TableCell>
                                    <TableCell className="text-right font-medium">{formatCurrency(invoice.totalAmount)}</TableCell>
                                    <TableCell className="text-center">{invoice.itemCount}</TableCell>
                                    <TableCell>
                                        <Badge variant={invoice.status === "COMPLETED" ? "default" : "destructive"}>
                                            {invoice.status}
                                        </Badge>
                                    </TableCell>
                                    <TableCell>
                                        <Button variant="ghost" size="icon" onClick={() => onViewDetail(invoice.id)}>
                                            <Eye className="h-4 w-4" />
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </div>

            {/* Pagination */}
            {data && data.totalPages > 1 && (
                <div className="flex items-center justify-between">
                    <p className="text-sm text-muted-foreground">
                        Page {page + 1} of {data.totalPages} ({data.totalElements} total)
                    </p>
                    <div className="flex gap-2">
                        <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                            <ChevronLeft className="h-4 w-4 mr-1" /> Previous
                        </Button>
                        <Button variant="outline" size="sm" disabled={page >= data.totalPages - 1} onClick={() => setPage(p => p + 1)}>
                            Next <ChevronRight className="h-4 w-4 ml-1" />
                        </Button>
                    </div>
                </div>
            )}
        </div>
    )
}
