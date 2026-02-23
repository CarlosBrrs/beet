"use client"

import * as React from "react"
import {
    ColumnDef,
    ColumnFiltersState,
    SortingState,
    VisibilityState,
    flexRender,
    getCoreRowModel,
    getFacetedRowModel,
    getFacetedUniqueValues,
    getFilteredRowModel,
    getPaginationRowModel,
    getSortedRowModel,
    useReactTable,
    PaginationState,
} from "@tanstack/react-table"

import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"

import { DataTablePagination } from "./data-table-pagination"
import { DataTableToolbar } from "./data-table-toolbar"

interface DataTableProps<TData, TValue> {
    columns: ColumnDef<TData, TValue>[]
    data: TData[]
    filterColumn?: string
    facets?: {
        column: string
        title: string
        options: {
            label: string
            value: string
            icon?: React.ComponentType<{ className?: string }>
        }[]
    }[]
    pageCount?: number
    pagination?: PaginationState
    onPaginationChange?: React.Dispatch<React.SetStateAction<PaginationState>>
    sorting?: SortingState
    onSortingChange?: React.Dispatch<React.SetStateAction<SortingState>>
    columnFilters?: ColumnFiltersState
    onColumnFiltersChange?: React.Dispatch<React.SetStateAction<ColumnFiltersState>>
    globalFilter?: string
    onGlobalFilterChange?: React.Dispatch<React.SetStateAction<string>>
}

export function DataTable<TData, TValue>({
    columns,
    data,
    filterColumn,
    facets,
    pageCount,
    pagination,
    onPaginationChange,
    sorting: controlledSorting,
    onSortingChange: controlledOnSortingChange,
    columnFilters: controlledColumnFilters,
    onColumnFiltersChange: controlledOnColumnFiltersChange,
    globalFilter: controlledGlobalFilter,
    onGlobalFilterChange: controlledOnGlobalFilterChange,
}: DataTableProps<TData, TValue>) {
    const isManual = pageCount !== undefined

    const [rowSelection, setRowSelection] = React.useState({})
    const [columnVisibility, setColumnVisibility] = React.useState<VisibilityState>({})

    const [internalColumnFilters, setInternalColumnFilters] = React.useState<ColumnFiltersState>([])
    const [internalSorting, setInternalSorting] = React.useState<SortingState>([])
    const [internalGlobalFilter, setInternalGlobalFilter] = React.useState("")

    const columnFilters = controlledColumnFilters ?? internalColumnFilters
    const setColumnFilters = controlledOnColumnFiltersChange ?? setInternalColumnFilters
    const sorting = controlledSorting ?? internalSorting
    const setSorting = controlledOnSortingChange ?? setInternalSorting
    const globalFilter = (controlledGlobalFilter !== undefined) ? controlledGlobalFilter : internalGlobalFilter
    const setGlobalFilter = controlledOnGlobalFilterChange ?? setInternalGlobalFilter

    const table = useReactTable({
        data,
        columns,
        pageCount,
        state: {
            sorting,
            columnVisibility,
            rowSelection,
            columnFilters,
            globalFilter,
            ...(pagination ? { pagination } : {})
        },
        manualPagination: isManual,
        manualSorting: isManual,
        manualFiltering: isManual,
        enableRowSelection: true,
        onRowSelectionChange: setRowSelection,
        onSortingChange: setSorting,
        onColumnFiltersChange: setColumnFilters,
        onColumnVisibilityChange: setColumnVisibility,
        onGlobalFilterChange: setGlobalFilter,
        ...(onPaginationChange ? { onPaginationChange } : {}),
        getCoreRowModel: getCoreRowModel(),
        getFilteredRowModel: getFilteredRowModel(),
        getPaginationRowModel: getPaginationRowModel(),
        getSortedRowModel: getSortedRowModel(),
        getFacetedRowModel: getFacetedRowModel(),
        getFacetedUniqueValues: getFacetedUniqueValues(),
    })

    return (
        <div className="space-y-4">
            <DataTableToolbar
                table={table}
                filterColumn={filterColumn}
                facets={facets}
            />
            <div className="rounded-md border">
                <Table>
                    <TableHeader>
                        {table.getHeaderGroups().map((headerGroup) => (
                            <TableRow key={headerGroup.id}>
                                {headerGroup.headers.map((header) => {
                                    return (
                                        <TableHead key={header.id} colSpan={header.colSpan}>
                                            {header.isPlaceholder
                                                ? null
                                                : flexRender(
                                                    header.column.columnDef.header,
                                                    header.getContext()
                                                )}
                                        </TableHead>
                                    )
                                })}
                            </TableRow>
                        ))}
                    </TableHeader>
                    <TableBody>
                        {table.getRowModel().rows?.length ? (
                            table.getRowModel().rows.map((row) => (
                                <TableRow
                                    key={row.id}
                                    data-state={row.getIsSelected() && "selected"}
                                >
                                    {row.getVisibleCells().map((cell) => (
                                        <TableCell key={cell.id}>
                                            {flexRender(
                                                cell.column.columnDef.cell,
                                                cell.getContext()
                                            )}
                                        </TableCell>
                                    ))}
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell
                                    colSpan={columns.length}
                                    className="h-24 text-center"
                                >
                                    No results.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </div>
            <DataTablePagination table={table} />
        </div>
    )
}
