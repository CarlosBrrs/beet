"use client"

import { ColumnDef } from "@tanstack/react-table"
import { CirclePlus, Loader2, MonitorSmartphone, Pencil, Power, PowerOff, Unplug } from "lucide-react"
import { useMemo, useState } from "react"

import { Can } from "@/components/shared/can"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { DataTable } from "@/components/shared/data-table/data-table"
import { DataTableColumnHeader } from "@/components/shared/data-table/data-table-column-header"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { CashRegisterResponse } from "@/lib/api-types"
import {
    useCashRegisters,
    useDeactivateCashRegister,
    useReleaseCashRegisterDeviceBinding,
    useUpdateCashRegister,
} from "@/lib/hooks/use-cash-registers"

import { CashRegisterDialog } from "./cash-register-dialog"

function shortDeviceId(deviceId: string) {
    return `${deviceId.slice(0, 8)}...`
}

function formatUpdatedAt(updatedAt: string) {
    return new Intl.DateTimeFormat("en", {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(new Date(updatedAt))
}

const createColumns = (
    onEdit: (register: CashRegisterResponse) => void,
    onDeactivate: (register: CashRegisterResponse) => void,
    onReactivate: (register: CashRegisterResponse) => void,
    onReleaseDevice: (register: CashRegisterResponse) => void,
): ColumnDef<CashRegisterResponse>[] => [
    {
        accessorKey: "name",
        header: ({ column }) => <DataTableColumnHeader column={column} title="Name" />,
    },
    {
        accessorKey: "deviceId",
        header: "Device",
        enableSorting: false,
        cell: ({ row }) => {
            const deviceId = row.original.deviceId
            return deviceId ? (
                <span className="flex items-center gap-1.5 font-mono" title={deviceId}>
                    <MonitorSmartphone className="h-3.5 w-3.5 text-muted-foreground" />
                    {shortDeviceId(deviceId)}
                </span>
            ) : (
                <span className="text-muted-foreground">Unbound</span>
            )
        },
    },
    {
        id: "status",
        accessorFn: (register) => register.isActive ? "Active" : "Inactive",
        header: ({ column }) => <DataTableColumnHeader column={column} title="Status" />,
        cell: ({ row }) => row.original.isActive ? (
            <Badge variant="outline" className="border-green-300 bg-green-50 text-green-700">
                Active
            </Badge>
        ) : (
            <Badge variant="secondary">Inactive</Badge>
        ),
    },
    {
        accessorKey: "notes",
        header: "Notes",
        enableSorting: false,
        cell: ({ row }) => (
            <span className="block max-w-[260px] truncate text-muted-foreground" title={row.original.notes ?? undefined}>
                {row.original.notes || "-"}
            </span>
        ),
    },
    {
        accessorKey: "updatedAt",
        header: ({ column }) => <DataTableColumnHeader column={column} title="Updated" />,
        cell: ({ row }) => formatUpdatedAt(row.original.updatedAt),
    },
    {
        id: "actions",
        cell: ({ row }) => {
            const register = row.original
            return (
                <div className="flex justify-end gap-1">
                    <Can I="EDIT" a="CASH">
                        <Button
                            variant="ghost"
                            size="icon"
                            title="Edit cash register"
                            onClick={() => onEdit(register)}
                        >
                            <Pencil className="h-4 w-4" />
                        </Button>
                    </Can>
                    {register.deviceId && (
                        <Can I="MANAGE" a="CASH">
                            <Button
                                variant="ghost"
                                size="icon"
                                title="Release device"
                                onClick={() => onReleaseDevice(register)}
                            >
                                <Unplug className="h-4 w-4" />
                            </Button>
                        </Can>
                    )}
                    {register.isActive ? (
                        <Can I="DELETE" a="CASH">
                            <Button
                                variant="ghost"
                                size="icon"
                                title="Deactivate cash register"
                                onClick={() => onDeactivate(register)}
                            >
                                <PowerOff className="h-4 w-4" />
                            </Button>
                        </Can>
                    ) : (
                        <Can I="EDIT" a="CASH">
                            <Button
                                variant="ghost"
                                size="icon"
                                title="Reactivate cash register"
                                onClick={() => onReactivate(register)}
                            >
                                <Power className="h-4 w-4" />
                            </Button>
                        </Can>
                    )}
                </div>
            )
        },
    },
]

export function CashRegisterList() {
    const { data: registers = [], isLoading, isError, error } = useCashRegisters()
    const deactivateRegister = useDeactivateCashRegister()
    const releaseDeviceBinding = useReleaseCashRegisterDeviceBinding()
    const updateRegister = useUpdateCashRegister()
    const [dialogOpen, setDialogOpen] = useState(false)
    const [editingRegister, setEditingRegister] = useState<CashRegisterResponse | null>(null)
    const [deactivationTarget, setDeactivationTarget] = useState<CashRegisterResponse | null>(null)
    const [releaseTarget, setReleaseTarget] = useState<CashRegisterResponse | null>(null)

    const columns = useMemo(
        () => createColumns(
            (register) => {
                setEditingRegister(register)
                setDialogOpen(true)
            },
            setDeactivationTarget,
            (register) => updateRegister.mutate({ id: register.id, request: { isActive: true } }),
            setReleaseTarget,
        ),
        [updateRegister]
    )

    const handleDialogOpenChange = (open: boolean) => {
        setDialogOpen(open)
        if (!open) setEditingRegister(null)
    }

    if (isLoading) {
        return (
            <div className="flex h-40 items-center justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
        )
    }

    if (isError) {
        return (
            <div className="border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
                {error.message || "Failed to load cash registers"}
            </div>
        )
    }

    return (
        <>
            <div className="space-y-4">
                <div className="flex justify-end">
                    <Can I="CREATE" a="CASH">
                        <Button
                            onClick={() => {
                                setEditingRegister(null)
                                setDialogOpen(true)
                            }}
                        >
                            <CirclePlus className="h-4 w-4" />
                            New cash register
                        </Button>
                    </Can>
                </div>
                <DataTable columns={columns} data={registers} />
            </div>

            <CashRegisterDialog
                register={editingRegister}
                open={dialogOpen}
                onOpenChange={handleDialogOpenChange}
            />

            <ConfirmDialog
                open={!!deactivationTarget}
                onOpenChange={(open) => {
                    if (!open) setDeactivationTarget(null)
                }}
                title="Deactivate cash register?"
                description={`The cash register "${deactivationTarget?.name ?? ""}" will no longer accept new sessions.`}
                confirmText="Deactivate"
                variant="destructive"
                onConfirm={async () => {
                    if (!deactivationTarget) return
                    await deactivateRegister.mutateAsync(deactivationTarget.id)
                }}
            />

            <ConfirmDialog
                open={!!releaseTarget}
                onOpenChange={(open) => {
                    if (!open) setReleaseTarget(null)
                }}
                title="Release assigned device?"
                description={`The cash register "${releaseTarget?.name ?? ""}" will be available for another device.`}
                confirmText="Release device"
                onConfirm={async () => {
                    if (!releaseTarget) return
                    await releaseDeviceBinding.mutateAsync(releaseTarget.id)
                }}
            />
        </>
    )
}
