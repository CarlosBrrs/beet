"use client"

import { ColumnDef } from "@tanstack/react-table"
import { MockIngredient } from "@/lib/api-types"
import { DataTable } from "@/components/shared/data-table/data-table"
import { Button } from "@/components/ui/button"
import { Can } from "@/components/shared/can"
import { PermissionModule, PermissionAction } from "@/lib/permissions"
import { Pencil, Trash2, Eye, BarChart2 } from "lucide-react"
import { DataTableColumnHeader } from "@/components/shared/data-table/data-table-column-header"
import { formatCurrency } from "@/lib/utils"

// Columns Definition
const createColumns = (
    onView: (ingredient: MockIngredient) => void,
    onEdit: (ingredient: MockIngredient) => void,
    onAdjust: (ingredient: MockIngredient) => void,
    onDelete: (ingredient: MockIngredient) => void
): ColumnDef<MockIngredient>[] => [
        {
            accessorKey: "name",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Name" />,
        },
        {
            accessorKey: "unit",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Unit" />,
        },
        {
            accessorKey: "cost",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Cost" />,
            cell: ({ row }) => formatCurrency(parseFloat(row.getValue("cost"))),
        },
        {
            accessorKey: "currentStock",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Stock" />,
        },
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
    ingredients: MockIngredient[]
    isLoading: boolean
    onView: (ingredient: MockIngredient) => void
    onEdit: (ingredient: MockIngredient) => void
    onAdjust: (ingredient: MockIngredient) => void
    onDelete: (ingredient: MockIngredient) => void
}

export function IngredientList({ ingredients, isLoading, onView, onEdit, onAdjust, onDelete }: IngredientListProps) {
    const columns = createColumns(onView, onEdit, onAdjust, onDelete)

    if (isLoading) return <div>Loading ingredients...</div>

    const facets = [
        {
            column: "unit",
            title: "Unit",
            options: [
                { label: "Kilogram (kg)", value: "kg" },
                { label: "Gram (g)", value: "g" },
                { label: "Liter (l)", value: "l" },
                { label: "Milliliter (ml)", value: "ml" },
                { label: "Unit (pcs)", value: "unit" },
            ],
        },
    ]

    return (
        <DataTable
            columns={columns}
            data={ingredients}
            facets={facets}
        />
    )
}
