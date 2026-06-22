"use client"

import { useMemo, useState } from "react"
import { ColumnDef, ColumnFiltersState, PaginationState, SortingState } from "@tanstack/react-table"
import { Plus, Pencil, Trash2, Power, PowerOff } from "lucide-react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { DataTable } from "@/components/shared/data-table/data-table"
import { DataTableColumnHeader } from "@/components/shared/data-table/data-table-column-header"
import { DialogShell } from "@/components/shared/dialog-shell"
import { SheetShell } from "@/components/shared/sheet-shell"
import { SupplierForm } from "@/components/modules/suppliers/supplier-form"
import { SupplierResponse } from "@/lib/api-types"
import {
    SupplierRequest,
    useCreateSupplier,
    useDeleteSupplier,
    useSetSupplierActive,
    useSupplierList,
    useUpdateSupplier,
} from "@/lib/hooks/use-suppliers"
import { useDebounce } from "@/lib/hooks/use-debounce"

function createColumns(
    onEdit: (supplier: SupplierResponse) => void,
    onToggleActive: (supplier: SupplierResponse) => void,
    onDelete: (supplier: SupplierResponse) => void
): ColumnDef<SupplierResponse>[] {
    return [
        {
            accessorKey: "name",
            header: ({ column }) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }) => (
                <div>
                    <p className="font-medium">{row.original.name}</p>
                    <p className="text-xs text-muted-foreground">{row.original.documentNumber}</p>
                </div>
            ),
        },
        {
            accessorKey: "contactName",
            header: "Contact",
            cell: ({ row }) => row.original.contactName || "-",
        },
        {
            accessorKey: "email",
            header: "Email",
            cell: ({ row }) => row.original.email || "-",
        },
        {
            accessorKey: "phone",
            header: "Phone",
            cell: ({ row }) => row.original.phone || "-",
        },
        {
            accessorKey: "isActive",
            header: "Status",
            cell: ({ row }) => (
                <Badge variant={row.original.isActive ? "default" : "secondary"}>
                    {row.original.isActive ? "Active" : "Inactive"}
                </Badge>
            ),
        },
        {
            id: "actions",
            cell: ({ row }) => {
                const supplier = row.original
                return (
                    <div className="flex items-center justify-end gap-2">
                        <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => onEdit(supplier)}>
                            <Pencil className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => onToggleActive(supplier)}>
                            {supplier.isActive ? <PowerOff className="h-4 w-4" /> : <Power className="h-4 w-4" />}
                        </Button>
                        <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive" onClick={() => onDelete(supplier)}>
                            <Trash2 className="h-4 w-4" />
                        </Button>
                    </div>
                )
            },
        },
    ]
}

export default function SuppliersPage() {
    const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 10 })
    const [sorting, setSorting] = useState<SortingState>([])
    const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
    const [globalFilter, setGlobalFilter] = useState("")
    const [sheetOpen, setSheetOpen] = useState(false)
    const [editingSupplier, setEditingSupplier] = useState<SupplierResponse | null>(null)
    const [activationTarget, setActivationTarget] = useState<SupplierResponse | null>(null)
    const [deleteTarget, setDeleteTarget] = useState<SupplierResponse | null>(null)

    const debouncedSearch = useDebounce(globalFilter, 500)
    const activeFilter = columnFilters.find((filter) => filter.id === "isActive")?.value as string[] | undefined
    const active = activeFilter?.length === 1 ? activeFilter[0] === "true" : undefined
    const sortBy = sorting.length > 0 ? sorting[0].id : undefined
    const sortDesc = sorting.length > 0 ? sorting[0].desc : undefined

    const { data: pageResult, isLoading } = useSupplierList({
        page: pagination.pageIndex,
        size: pagination.pageSize,
        search: debouncedSearch,
        active,
        sortBy,
        sortDesc,
    })

    const createMutation = useCreateSupplier()
    const updateMutation = useUpdateSupplier()
    const activeMutation = useSetSupplierActive()
    const deleteMutation = useDeleteSupplier()

    const columns = useMemo(
        () => createColumns(
            (supplier) => {
                setEditingSupplier(supplier)
                setSheetOpen(true)
            },
            setActivationTarget,
            setDeleteTarget
        ),
        []
    )

    const handleSubmit = (values: SupplierRequest) => {
        if (editingSupplier) {
            updateMutation.mutate({ id: editingSupplier.id, payload: values }, {
                onSuccess: () => {
                    toast.success("Supplier updated")
                    setSheetOpen(false)
                    setEditingSupplier(null)
                },
                onError: (error) => toast.error(error.message || "Failed to update supplier"),
            })
            return
        }

        createMutation.mutate(values, {
            onSuccess: () => {
                toast.success("Supplier created")
                setSheetOpen(false)
            },
            onError: (error) => toast.error(error.message || "Failed to create supplier"),
        })
    }

    const confirmActivation = () => {
        if (!activationTarget) return
        activeMutation.mutate({ id: activationTarget.id, isActive: !activationTarget.isActive }, {
            onSuccess: () => {
                toast.success(activationTarget.isActive ? "Supplier deactivated" : "Supplier reactivated")
                setActivationTarget(null)
            },
            onError: (error) => toast.error(error.message || "Failed to update supplier status"),
        })
    }

    const confirmDelete = () => {
        if (!deleteTarget) return
        deleteMutation.mutate(deleteTarget.id, {
            onSuccess: () => {
                toast.success("Supplier deleted")
                setDeleteTarget(null)
            },
            onError: (error) => toast.error(error.message || "Failed to delete supplier"),
        })
    }

    const suppliers = pageResult?.content ?? []
    const pageCount = pageResult?.totalPages ?? -1

    return (
        <div className="space-y-4 pb-8">
            <div className="flex items-center justify-between gap-4">
                <div>
                    <h2 className="text-2xl font-bold tracking-tight">Suppliers</h2>
                    <p className="text-muted-foreground">Manage the shared supplier catalog used by ingredients and purchases.</p>
                </div>
                <Button onClick={() => { setEditingSupplier(null); setSheetOpen(true) }}>
                    <Plus className="mr-2 h-4 w-4" /> Add Supplier
                </Button>
            </div>

            {isLoading && suppliers.length === 0 ? (
                <div className="text-sm text-muted-foreground">Loading suppliers...</div>
            ) : (
                <DataTable
                    columns={columns}
                    data={suppliers}
                    pageCount={pageCount}
                    pagination={pagination}
                    onPaginationChange={setPagination}
                    sorting={sorting}
                    onSortingChange={setSorting}
                    columnFilters={columnFilters}
                    onColumnFiltersChange={setColumnFilters}
                    globalFilter={globalFilter}
                    onGlobalFilterChange={setGlobalFilter}
                    facets={[
                        {
                            column: "isActive",
                            title: "Status",
                            options: [
                                { label: "Active", value: "true" },
                                { label: "Inactive", value: "false" },
                            ],
                        },
                    ]}
                />
            )}

            <SheetShell
                open={sheetOpen}
                onOpenChange={(open) => {
                    setSheetOpen(open)
                    if (!open) setEditingSupplier(null)
                }}
                title={editingSupplier ? "Edit Supplier" : "New Supplier"}
                description={editingSupplier ? "Update supplier metadata." : "Create a supplier for ingredient purchases."}
                size="lg"
            >
                <SupplierForm
                    supplier={editingSupplier}
                    isSubmitting={createMutation.isPending || updateMutation.isPending}
                    onSubmit={handleSubmit}
                />
            </SheetShell>

            {activationTarget && (
                <DialogShell
                    open={!!activationTarget}
                    onOpenChange={(open) => !open && setActivationTarget(null)}
                    title={activationTarget.isActive ? "Deactivate supplier" : "Reactivate supplier"}
                    description={activationTarget.isActive ? "Suppliers with purchases or ingredient presentations cannot be deactivated." : undefined}
                >
                    <div className="space-y-4">
                        <p className="text-sm">
                            {activationTarget.isActive ? "Deactivate" : "Reactivate"} <span className="font-medium">{activationTarget.name}</span>?
                        </p>
                        <div className="flex justify-end gap-2">
                            <Button variant="outline" onClick={() => setActivationTarget(null)}>Cancel</Button>
                            <Button onClick={confirmActivation} disabled={activeMutation.isPending}>
                                Confirm
                            </Button>
                        </div>
                    </div>
                </DialogShell>
            )}

            {deleteTarget && (
                <DialogShell
                    open={!!deleteTarget}
                    onOpenChange={(open) => !open && setDeleteTarget(null)}
                    title="Delete supplier"
                    description="This performs a soft delete only if the supplier has no purchases or ingredient presentations linked."
                >
                    <div className="space-y-4">
                        <p className="text-sm">
                            Delete <span className="font-medium">{deleteTarget.name}</span>?
                        </p>
                        <div className="flex justify-end gap-2">
                            <Button variant="outline" onClick={() => setDeleteTarget(null)}>Cancel</Button>
                            <Button variant="destructive" onClick={confirmDelete} disabled={deleteMutation.isPending}>
                                Delete
                            </Button>
                        </div>
                    </div>
                </DialogShell>
            )}
        </div>
    )
}
