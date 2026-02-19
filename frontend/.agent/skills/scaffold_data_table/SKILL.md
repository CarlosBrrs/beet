---
name: scaffold_data_table
description: Generates the generic DataTable component structure with sorting, pagination, and filtering.
---

# Scaffold Data Table

This skill generates the reusable `DataTable` component ecosystem based on `shadcn/ui` and `@tanstack/react-table`.

## Usage
Run this when initializing the generic UI components folder.

## Actions

1.  **Base Component**: `components/shared/data-table/data-table.tsx`
2.  **Pagination**: `components/shared/data-table/data-table-pagination.tsx`
3.  **Toolbar**: `components/shared/data-table/data-table-toolbar.tsx`
4.  **Faceted Filter**: `components/shared/data-table/data-table-faceted-filter.tsx`

## Prerequisites
Ensure `shadcn` table, button, dropdown-menu, etc., are installed.

## Template: `data-table.tsx`

```tsx
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
} from "@tanstack/react-table"

import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { DataTablePagination } from "./data-table-pagination"
import { DataTableToolbar } from "./data-table-toolbar"

interface DataTableProps<TData, TValue> {
    columns: ColumnDef<TData, TValue>[]
    data: TData[]
    filterColumn?: string
    // Add Facets prop definition here
}

export function DataTable<TData, TValue>({
    columns,
    data,
    filterColumn,
}: DataTableProps<TData, TValue>) {
    // ... Implement standard TanStack Table logic ...
    
    return (
        <div className="space-y-4">
            <DataTableToolbar table={table} filterColumn={filterColumn} />
            <div className="rounded-md border">
                <Table>
                    {/* Render Header and Body */}
                </Table>
            </div>
            <DataTablePagination table={table} />
        </div>
    )
}
```
