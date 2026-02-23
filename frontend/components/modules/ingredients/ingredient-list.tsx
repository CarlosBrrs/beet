"use client"

import { ColumnDef } from "@tanstack/react-table"
import { IngredientListResponse, MockIngredient } from "@/lib/api-types"
import { DataTable } from "@/components/shared/data-table/data-table"
import { Button } from "@/components/ui/button"
import { Can } from "@/components/shared/can"
import { PermissionModule, PermissionAction } from "@/lib/permissions"
import { Pencil, Trash2, Eye, BarChart2 } from "lucide-react"
import { useState } from "react"
import { DataTableColumnHeader } from "@/components/shared/data-table/data-table-column-header"
import { formatCurrency } from "@/lib/utils"
import { useIngredients } from "@/lib/hooks/use-ingredients"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { useDebounce } from "@/lib/hooks/use-debounce"
import { PaginationState, SortingState, ColumnFiltersState } from "@tanstack/react-table"

// Columns Definition
const createColumns = (
    onView: (ingredient: IngredientListResponse) => void,
    onEdit: (ingredient: IngredientListResponse) => void,
    onAdjust: (ingredient: IngredientListResponse) => void,
    onDelete: (ingredient: IngredientListResponse) => void
): ColumnDef<IngredientListResponse>[] => [
        {
            accessorKey: "name",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Name" />,
        },
        {
            accessorKey: "unitAbbreviation",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Unit" />,
        },
        {
            accessorKey: "costPerBaseUnit",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Cost" />,
            cell: ({ row }) => {
                const val = row.getValue("costPerBaseUnit")
                return val ? formatCurrency(parseFloat(val as string)) : "-"
            },
        },
        // {
        //     accessorKey: "currentStock",
        //     header: ({ column }) => <DataTableColumnHeader column={column} title="Stock" />,
        // },
        {
            id: "actions",
            cell: ({ row }) => {
                const ingredient = row.original
                return (
                    <div className="flex items-center gap-2">
                        <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => onView(ingredient)}>
                            <Eye className="h-4 w-4" />
                        </Button>
                        <Can I={PermissionAction.EDIT} a={PermissionModule.INVENTORY}>
                            <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => onEdit(ingredient)}>
                                <Pencil className="h-4 w-4" />
                            </Button>
                        </Can>
                        <Can I={PermissionAction.EDIT} a={PermissionModule.INVENTORY}>
                            <Button variant="ghost" size="icon" className="h-8 w-8 text-blue-500" onClick={() => onAdjust(ingredient)}>
                                <BarChart2 className="h-4 w-4" />
                            </Button>
                        </Can>
                        <Can I={PermissionAction.DELETE} a={PermissionModule.INVENTORY}>
                            <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive" onClick={() => onDelete(ingredient)}>
                                <Trash2 className="h-4 w-4" />
                            </Button>
                        </Can>
                    </div>
                )
            },
        },
    ]

interface IngredientListProps {
    onView: (ingredient: IngredientListResponse) => void
    onEdit: (ingredient: IngredientListResponse) => void
    onAdjust: (ingredient: IngredientListResponse) => void
    onDelete: (ingredient: IngredientListResponse) => void
}

export function IngredientList({ onView, onEdit, onAdjust, onDelete }: IngredientListProps) {
    const { restaurantId } = useRestaurantContext()

    // Controlled Server-Side States
    const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 10 })
    const [sorting, setSorting] = useState<SortingState>([])
    const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
    const [globalFilter, setGlobalFilter] = useState("")

    // Debounce the search input by 500ms
    const debouncedSearch = useDebounce(globalFilter, 500)

    // Parse TanStack Table state into backend API expected params
    const sortBy = sorting.length > 0 ? sorting[0].id : undefined
    const sortDesc = sorting.length > 0 ? sorting[0].desc : undefined

    const unitFilter = columnFilters.find(f => f.id === "unitAbbreviation")
    const units = unitFilter ? (unitFilter.value as string[]) : undefined

    // Fetch paginated simulated data
    const { data: pageResult, isLoading } = useIngredients({
        page: pagination.pageIndex,
        size: pagination.pageSize,
        search: debouncedSearch,
        sortBy,
        sortDesc,
        units
    })

    const ingredients = pageResult?.content || []
    const pageCount = pageResult?.totalPages || -1

    const columns = createColumns(onView, onEdit, onAdjust, onDelete)

    if (isLoading && ingredients.length === 0) return <div>Loading ingredients...</div>

    const facets = [
        {
            column: "unitAbbreviation",
            title: "Unit",
            options: [
                { label: "Kilogram (kg)", value: "kg" },
                { label: "Gram (g)", value: "g" },
                { label: "Liter (l)", value: "l" },
                { label: "Milliliter (ml)", value: "ml" },
                { label: "Piece (pcs)", value: "pcs" },
            ],
        },
    ]

    return (
        <DataTable
            columns={columns}
            data={ingredients}
            facets={facets}
            pageCount={pageCount}
            pagination={pagination}
            onPaginationChange={setPagination}
            sorting={sorting}
            onSortingChange={setSorting}
            columnFilters={columnFilters}
            onColumnFiltersChange={setColumnFilters}
            globalFilter={globalFilter}
            onGlobalFilterChange={setGlobalFilter}
        />
    )
}
