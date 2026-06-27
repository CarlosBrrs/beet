"use client"

import {
    CirclePlus,
    LayoutGrid,
    MapPin,
    Pencil,
    Power,
    PowerOff,
    Search,
    Users,
} from "lucide-react"
import { useMemo, useState } from "react"

import { Can } from "@/components/shared/can"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { RestaurantTableResponse, TableAvailabilityStatus } from "@/lib/api-types"
import {
    useDeactivateRestaurantTable,
    useRestaurantTables,
    useUpdateRestaurantTable,
} from "@/lib/hooks/use-restaurant-tables"
import { cn } from "@/lib/utils"

import { RestaurantTableDialog } from "./restaurant-table-dialog"

type StatusFilter = "ACTIVE" | TableAvailabilityStatus | "ALL"

const statusOptions: { value: StatusFilter; label: string }[] = [
    { value: "ACTIVE", label: "Active" },
    { value: "AVAILABLE", label: "Available" },
    { value: "OCCUPIED", label: "Occupied" },
    { value: "INACTIVE", label: "Inactive" },
    { value: "ALL", label: "All" },
]

export function RestaurantTableGrid() {
    const { data: tables = [], isLoading, isError, error } = useRestaurantTables()
    const deactivateTable = useDeactivateRestaurantTable()
    const updateTable = useUpdateRestaurantTable()
    const [search, setSearch] = useState("")
    const [area, setArea] = useState("ALL")
    const [status, setStatus] = useState<StatusFilter>("ACTIVE")
    const [dialogOpen, setDialogOpen] = useState(false)
    const [editingTable, setEditingTable] = useState<RestaurantTableResponse | null>(null)
    const [deactivationTarget, setDeactivationTarget] = useState<RestaurantTableResponse | null>(null)

    const areas = useMemo(
        () => [...new Set(tables.map((table) => table.area).filter((value): value is string => !!value))]
            .sort((left, right) => left.localeCompare(right)),
        [tables]
    )
    const visibleTables = useMemo(() => {
        const normalizedSearch = search.trim().toLocaleLowerCase()
        return tables.filter((table) => {
            const matchesSearch = !normalizedSearch
                || table.name.toLocaleLowerCase().includes(normalizedSearch)
                || table.area?.toLocaleLowerCase().includes(normalizedSearch)
            const matchesArea = area === "ALL" || table.area === area
            const matchesStatus = status === "ALL"
                || (status === "ACTIVE" && table.availabilityStatus !== "INACTIVE")
                || table.availabilityStatus === status
            return matchesSearch && matchesArea && matchesStatus
        })
    }, [area, search, status, tables])

    const handleDialogOpenChange = (open: boolean) => {
        setDialogOpen(open)
        if (!open) setEditingTable(null)
    }

    if (isLoading) return <TableGridSkeleton />

    if (isError) {
        return (
            <div className="border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
                {error.message || "Failed to load tables"}
            </div>
        )
    }

    return (
        <>
            <section className="space-y-4">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                    <div className="flex flex-1 flex-col gap-2 sm:flex-row">
                        <label className="relative max-w-sm flex-1">
                            <Search className="pointer-events-none absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                            <Input
                                value={search}
                                onChange={(event) => setSearch(event.target.value)}
                                placeholder="Search tables"
                                className="pl-8"
                            />
                        </label>
                        <FilterSelect value={area} onValueChange={setArea}>
                            <SelectItem value="ALL">All areas</SelectItem>
                            {areas.map((option) => <SelectItem key={option} value={option}>{option}</SelectItem>)}
                        </FilterSelect>
                        <FilterSelect value={status} onValueChange={(value) => setStatus(value as StatusFilter)}>
                            {statusOptions.map((option) => (
                                <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>
                            ))}
                        </FilterSelect>
                    </div>
                    <Can I="CREATE" a="TABLES">
                        <Button onClick={() => setDialogOpen(true)}>
                            <CirclePlus className="h-4 w-4" />
                            New table
                        </Button>
                    </Can>
                </div>

                {visibleTables.length === 0 ? (
                    <div className="flex min-h-40 flex-col items-center justify-center gap-2 border text-center text-sm text-muted-foreground">
                        <LayoutGrid className="h-5 w-5" />
                        No tables match the current filters.
                    </div>
                ) : (
                    <div className="grid grid-cols-[repeat(auto-fill,minmax(210px,1fr))] gap-3">
                        {visibleTables.map((table) => (
                            <TableBlock
                                key={table.id}
                                table={table}
                                onEdit={() => {
                                    setEditingTable(table)
                                    setDialogOpen(true)
                                }}
                                onDeactivate={() => setDeactivationTarget(table)}
                                onReactivate={() => updateTable.mutate({ id: table.id, request: { isActive: true } })}
                            />
                        ))}
                    </div>
                )}
            </section>

            <RestaurantTableDialog table={editingTable} open={dialogOpen} onOpenChange={handleDialogOpenChange} />
            <ConfirmDialog
                open={!!deactivationTarget}
                onOpenChange={(open) => {
                    if (!open) setDeactivationTarget(null)
                }}
                title="Deactivate table?"
                description={`The table "${deactivationTarget?.name ?? ""}" will no longer accept dine-in orders.`}
                confirmText="Deactivate"
                variant="destructive"
                onConfirm={async () => {
                    if (!deactivationTarget) return
                    await deactivateTable.mutateAsync(deactivationTarget.id)
                }}
            />
        </>
    )
}

function TableBlock({
    table,
    onEdit,
    onDeactivate,
    onReactivate,
}: {
    table: RestaurantTableResponse
    onEdit: () => void
    onDeactivate: () => void
    onReactivate: () => void
}) {
    const occupied = table.availabilityStatus === "OCCUPIED"
    return (
        <article className={cn(
            "flex min-h-44 flex-col justify-between border p-4",
            occupied && "border-amber-300 bg-amber-50/50",
            table.availabilityStatus === "INACTIVE" && "bg-muted/40 text-muted-foreground"
        )}>
            <div className="space-y-3">
                <div className="flex items-start justify-between gap-2">
                    <h2 className="truncate text-sm font-semibold" title={table.name}>{table.name}</h2>
                    <StatusBadge status={table.availabilityStatus} />
                </div>
                <div className="space-y-1.5 text-xs text-muted-foreground">
                    <p className="flex items-center gap-1.5">
                        <Users className="h-3.5 w-3.5" />
                        Capacity {table.capacity}
                    </p>
                    <p className="flex items-center gap-1.5">
                        <MapPin className="h-3.5 w-3.5" />
                        {table.area || "No area"}
                    </p>
                </div>
                {occupied && <p className="text-xs font-medium text-amber-800">Open order assigned</p>}
            </div>
            <div className="flex justify-end gap-1 pt-3">
                <Can I="EDIT" a="TABLES">
                    <Button variant="ghost" size="icon" title="Edit table" onClick={onEdit}>
                        <Pencil className="h-4 w-4" />
                    </Button>
                </Can>
                {table.isActive ? (
                    <Can I="DELETE" a="TABLES">
                        <Button
                            variant="ghost"
                            size="icon"
                            title={occupied ? "Close or cancel the open order before deactivating" : "Deactivate table"}
                            disabled={occupied}
                            onClick={onDeactivate}
                        >
                            <PowerOff className="h-4 w-4" />
                        </Button>
                    </Can>
                ) : (
                    <Can I="EDIT" a="TABLES">
                        <Button variant="ghost" size="icon" title="Reactivate table" onClick={onReactivate}>
                            <Power className="h-4 w-4" />
                        </Button>
                    </Can>
                )}
            </div>
        </article>
    )
}

function StatusBadge({ status }: { status: TableAvailabilityStatus }) {
    if (status === "AVAILABLE") {
        return <Badge variant="outline" className="border-green-300 bg-green-50 text-green-700">Available</Badge>
    }
    if (status === "OCCUPIED") {
        return <Badge variant="outline" className="border-amber-300 bg-amber-100 text-amber-800">Occupied</Badge>
    }
    return <Badge variant="secondary">Inactive</Badge>
}

function FilterSelect({
    value,
    onValueChange,
    children,
}: {
    value: string
    onValueChange: (value: string) => void
    children: React.ReactNode
}) {
    return (
        <Select value={value} onValueChange={onValueChange}>
            <SelectTrigger className="w-full sm:w-40">
                <SelectValue />
            </SelectTrigger>
            <SelectContent>{children}</SelectContent>
        </Select>
    )
}

function TableGridSkeleton() {
    return (
        <div className="grid grid-cols-[repeat(auto-fill,minmax(210px,1fr))] gap-3">
            {Array.from({ length: 6 }).map((_, index) => (
                <Skeleton key={index} className="h-44 border" />
            ))}
        </div>
    )
}
