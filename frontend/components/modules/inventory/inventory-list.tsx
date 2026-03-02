"use client"

import { ColumnDef } from "@tanstack/react-table"
import { InventoryStockResponse } from "@/lib/api-types"
import { DataTable } from "@/components/shared/data-table/data-table"
import { Button } from "@/components/ui/button"
import { Can } from "@/components/shared/can"
import { Pencil, Clock, AlertTriangle, CheckCircle } from "lucide-react"
import { useState } from "react"
import { DataTableColumnHeader } from "@/components/shared/data-table/data-table-column-header"
import { useInventoryStocks } from "@/lib/hooks/use-inventory"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { useDebounce } from "@/lib/hooks/use-debounce"
import { PaginationState, SortingState } from "@tanstack/react-table"
import { Badge } from "@/components/ui/badge"

// Columns Definition
const createColumns = (
    onAdjust: (stock: InventoryStockResponse) => void,
    onHistory: (stock: InventoryStockResponse) => void,
): ColumnDef<InventoryStockResponse>[] => [
        {
            accessorKey: "ingredientName",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Ingredient" />,
        },
        {
            accessorKey: "unitAbbreviation",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Unit" />,
            enableSorting: false,
        },
        {
            accessorKey: "currentStock",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Stock" />,
            cell: ({ row }) => {
                const val = row.getValue("currentStock") as number
                return val !== null && val !== undefined ? val.toFixed(2) : "-"
            },
        },
        {
            accessorKey: "minStock",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Min. Stock" />,
            enableSorting: false,
            cell: ({ row }) => {
                const val = row.getValue("minStock") as number
                return val !== null && val !== undefined ? val.toFixed(2) : "-"
            },
        },
        {
            id: "status",
            header: "Status",
            cell: ({ row }) => {
                const stock = row.original
                return stock.lowStock ? (
                    <Badge variant="destructive" className="gap-1">
                        <AlertTriangle className="h-3 w-3" /> Low
                    </Badge>
                ) : (
                    <Badge variant="outline" className="gap-1 text-green-600 border-green-300 bg-green-50">
                        <CheckCircle className="h-3 w-3" /> OK
                    </Badge>
                )
            },
        },
        {
            id: "actions",
            cell: ({ row }) => {
                const stock = row.original
                return (
                    <div className="flex items-center gap-1">
                        <Can I="EDIT" a="INVENTORY">
                            <Button variant="ghost" size="icon" className="h-8 w-8"
                                onClick={() => onAdjust(stock)}
                                title="Adjust Stock"
                            >
                                <Pencil className="h-4 w-4" />
                            </Button>
                        </Can>
                        <Button variant="ghost" size="icon" className="h-8 w-8"
                            onClick={() => onHistory(stock)}
                            title="View History"
                        >
                            <Clock className="h-4 w-4" />
                        </Button>
                    </div>
                )
            },
        },
    ]

interface InventoryListProps {
    onAdjust: (stock: InventoryStockResponse) => void
    onHistory: (stock: InventoryStockResponse) => void
}

export function InventoryList({ onAdjust, onHistory }: InventoryListProps) {
    const { restaurantId } = useRestaurantContext()

    // Controlled Server-Side States
    const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 10 })
    const [sorting, setSorting] = useState<SortingState>([])
    const [globalFilter, setGlobalFilter] = useState("")

    // Debounce the search input by 500ms
    const debouncedSearch = useDebounce(globalFilter, 500)

    // Parse TanStack Table state into backend API expected params
    const sortBy = sorting.length > 0 ? sorting[0].id : undefined
    const sortDesc = sorting.length > 0 ? sorting[0].desc : undefined

    const { data: pageResult, isLoading } = useInventoryStocks({
        restaurantId: restaurantId!,
        page: pagination.pageIndex,
        size: pagination.pageSize,
        search: debouncedSearch,
        sortBy,
        sortDesc,
    })

    const stocks = pageResult?.content || []
    const pageCount = pageResult?.totalPages || -1

    const columns = createColumns(onAdjust, onHistory)

    if (isLoading && stocks.length === 0) return <div>Loading inventory...</div>

    return (
        <DataTable
            columns={columns}
            data={stocks}
            pageCount={pageCount}
            pagination={pagination}
            onPaginationChange={setPagination}
            sorting={sorting}
            onSortingChange={setSorting}
            globalFilter={globalFilter}
            onGlobalFilterChange={setGlobalFilter}
        />
    )
}
