"use client"

import { useState } from "react"
import { Copy, Edit, Plus, UserMinus, UserRoundCheck, UserRoundX } from "lucide-react"
import { toast } from "sonner"

import { useOptionalRestaurantContext } from "@/components/providers/restaurant-provider"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { DialogShell } from "@/components/shared/dialog-shell"
import { ListPagination } from "@/components/shared/list-pagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { StaffInvitationResponse, StaffMemberResponse, StaffRoleResponse } from "@/lib/api-types"
import { useDebounce } from "@/lib/hooks/use-debounce"
import {
    useAccountStaff,
    useAccountStaffMutations,
    useRestaurantStaff,
    useStaffInvitations,
    useStaffMutations,
    useStaffRoles,
} from "@/lib/hooks/use-staff"

interface StaffWorkspaceProps {
    mode: "restaurant" | "account"
}

type ConfirmState =
    | { type: "remove-assignment"; userId: string; label: string }
    | { type: "account-status"; userId: string; label: string; status: "ACTIVE" | "SUSPENDED" }
    | { type: "invitation-revoke"; invitationId: string; label: string }
    | null

export function StaffWorkspace({ mode }: StaffWorkspaceProps) {
    const restaurant = useOptionalRestaurantContext()
    const restaurantId = restaurant?.restaurantId ?? ""
    const isRestaurant = mode === "restaurant"
    const [tab, setTab] = useState(isRestaurant ? "employees" : "account")
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(20)
    const [search, setSearch] = useState("")
    const [status, setStatus] = useState("ALL")
    const [roleFilter, setRoleFilter] = useState("ALL")
    const [inviteOpen, setInviteOpen] = useState(false)
    const [assignmentEdit, setAssignmentEdit] = useState<StaffMemberResponse | null>(null)
    const [confirm, setConfirm] = useState<ConfirmState>(null)
    const debouncedSearch = useDebounce(search, 300)

    const employeeParams = {
        page,
        size,
        search: debouncedSearch || undefined,
        status: status === "ALL" ? undefined : status,
        roleId: roleFilter === "ALL" ? undefined : roleFilter,
    }

    const invitationParams = {
        page,
        size,
        search: debouncedSearch || undefined,
    }

    const accountParams = {
        page,
        size,
        search: debouncedSearch || undefined,
        status: status === "ALL" ? undefined : status,
    }

    const roles = useStaffRoles(restaurantId)
    const restaurantStaff = useRestaurantStaff(restaurantId, employeeParams)
    const invitations = useStaffInvitations(restaurantId, invitationParams)
    const accountStaff = useAccountStaff(accountParams)
    const staffMutations = useStaffMutations(restaurantId)
    const accountMutations = useAccountStaffMutations()

    const activeRoles = (roles.data ?? []).filter((role) => role.active)
    const staffData = isRestaurant ? restaurantStaff.data : accountStaff.data

    const resetPaging = () => setPage(0)

    const handleConfirm = async () => {
        if (!confirm) return
        try {
            if (confirm.type === "remove-assignment") {
                await staffMutations.removeAssignment.mutateAsync(confirm.userId)
                toast.success("Access removed")
            }
            if (confirm.type === "account-status") {
                await accountMutations.updateAccountStatus.mutateAsync({
                    userId: confirm.userId,
                    status: confirm.status,
                })
                toast.success(confirm.status === "SUSPENDED" ? "Account suspended" : "Account reactivated")
            }
            if (confirm.type === "invitation-revoke") {
                await staffMutations.revokeInvitation.mutateAsync({ invitationId: confirm.invitationId })
                toast.success("Invitation revoked")
            }
            setConfirm(null)
        } catch (error) {
            toast.error(error instanceof Error ? error.message : "Operation failed")
        }
    }

    return (
        <div className="space-y-6 pb-12">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">
                        {isRestaurant ? "Staff" : "Account staff"}
                    </h1>
                    <p className="mt-1 text-sm text-muted-foreground">
                        {isRestaurant
                            ? "Manage employees and invitations. Role permissions are defined by the platform."
                            : "Review employees across all restaurants and suspend account access when needed."}
                    </p>
                </div>
                {isRestaurant && (
                    <Button onClick={() => setInviteOpen(true)}>
                        <Plus className="mr-2 h-4 w-4" /> Invite
                    </Button>
                )}
            </div>

            <div className="flex flex-col gap-3 border p-4 sm:flex-row">
                <Input
                    value={search}
                    onChange={(event) => { setSearch(event.target.value); resetPaging() }}
                    placeholder="Search by name or email"
                    className="sm:max-w-sm"
                />
                {tab !== "roles" && (
                    <Select value={status} onValueChange={(value) => { setStatus(value); resetPaging() }}>
                        <SelectTrigger className="sm:w-44"><SelectValue /></SelectTrigger>
                        <SelectContent>
                            <SelectItem value="ALL">All statuses</SelectItem>
                            <SelectItem value="ACTIVE">Active</SelectItem>
                            <SelectItem value="SUSPENDED">Suspended</SelectItem>
                        </SelectContent>
                    </Select>
                )}
                {isRestaurant && tab === "employees" && (
                    <Select value={roleFilter} onValueChange={(value) => { setRoleFilter(value); resetPaging() }}>
                        <SelectTrigger className="sm:w-44"><SelectValue /></SelectTrigger>
                        <SelectContent>
                            <SelectItem value="ALL">All roles</SelectItem>
                            {(roles.data ?? []).map((role) => (
                                <SelectItem key={role.id} value={role.id}>{role.name}</SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                )}
            </div>

            <Tabs value={tab} onValueChange={(value) => { setTab(value); setStatus("ALL"); resetPaging() }}>
                {isRestaurant && (
                    <TabsList variant="line">
                        <TabsTrigger value="employees">Employees</TabsTrigger>
                        <TabsTrigger value="roles">Roles</TabsTrigger>
                        <TabsTrigger value="invitations">Invitations</TabsTrigger>
                    </TabsList>
                )}

                <TabsContent value={isRestaurant ? "employees" : "account"} className="space-y-3 pt-4">
                    <StaffTable
                        rows={staffData?.content ?? []}
                        mode={mode}
                        onEdit={(member) => setAssignmentEdit(member)}
                        onRemove={(member) => setConfirm({
                            type: "remove-assignment",
                            userId: member.userId,
                            label: member.fullName || member.email,
                        })}
                        onAccountStatus={(member, nextStatus) => setConfirm({
                            type: "account-status",
                            userId: member.userId,
                            label: member.fullName || member.email,
                            status: nextStatus,
                        })}
                    />
                    <ListPagination
                        totalElements={staffData?.totalElements ?? 0}
                        totalPages={staffData?.totalPages ?? 0}
                        page={page}
                        size={size}
                        label="employees"
                        onPageChange={setPage}
                        onSizeChange={(value) => { setSize(value); resetPaging() }}
                    />
                </TabsContent>

                {isRestaurant && (
                    <>
                        <TabsContent value="roles" className="space-y-3 pt-4">
                            <RolesTable rows={roles.data ?? []} />
                        </TabsContent>
                        <TabsContent value="invitations" className="space-y-3 pt-4">
                            <InvitationsTable
                                rows={invitations.data?.content ?? []}
                                onCopy={(path) => {
                                    const url = `${window.location.origin}${path}`
                                    void navigator.clipboard.writeText(url)
                                    toast.success("Invitation link copied")
                                }}
                                onRegenerate={async (id) => {
                                    try {
                                        const regenerated = await staffMutations.regenerateInvitation.mutateAsync(id)
                                        if (regenerated.invitationPath) {
                                            await navigator.clipboard.writeText(`${window.location.origin}${regenerated.invitationPath}`)
                                            toast.success("New invitation link copied")
                                        }
                                    } catch (error) {
                                        toast.error(error instanceof Error ? error.message : "Could not regenerate")
                                    }
                                }}
                                onRevoke={(row) => setConfirm({
                                    type: "invitation-revoke",
                                    invitationId: row.id,
                                    label: row.email,
                                })}
                            />
                            <ListPagination
                                totalElements={invitations.data?.totalElements ?? 0}
                                totalPages={invitations.data?.totalPages ?? 0}
                                page={page}
                                size={size}
                                label="invitations"
                                onPageChange={setPage}
                                onSizeChange={(value) => { setSize(value); resetPaging() }}
                            />
                        </TabsContent>
                    </>
                )}
            </Tabs>

            {isRestaurant && (
                <>
                    <InviteDialog
                        open={inviteOpen}
                        roles={activeRoles}
                        onOpenChange={setInviteOpen}
                        onSubmit={async (payload) => {
                            try {
                                const invitation = await staffMutations.invite.mutateAsync(payload)
                                if (invitation?.invitationPath) {
                                    await navigator.clipboard.writeText(`${window.location.origin}${invitation.invitationPath}`)
                                    toast.success("Invitation created and link copied")
                                } else {
                                    toast.success("Existing employee assigned")
                                }
                                setInviteOpen(false)
                            } catch (error) {
                                toast.error(error instanceof Error ? error.message : "Could not invite employee")
                            }
                        }}
                    />
                    <AssignmentDialog
                        key={assignmentEdit?.userId ?? "closed"}
                        open={!!assignmentEdit}
                        member={assignmentEdit}
                        roles={activeRoles}
                        onOpenChange={(open) => !open && setAssignmentEdit(null)}
                        onSubmit={async (userId, payload) => {
                            try {
                                await staffMutations.updateAssignment.mutateAsync({ userId, payload })
                                toast.success("Employee updated")
                                setAssignmentEdit(null)
                            } catch (error) {
                                toast.error(error instanceof Error ? error.message : "Could not update employee")
                            }
                        }}
                    />
                </>
            )}

            <ConfirmDialog
                open={!!confirm}
                onOpenChange={(open) => !open && setConfirm(null)}
                title="Confirm action"
                description={confirm ? confirmationText(confirm) : ""}
                confirmText="Confirm"
                variant="destructive"
                onConfirm={handleConfirm}
            />
        </div>
    )
}

function StaffTable({
    rows, mode, onEdit, onRemove, onAccountStatus,
}: {
    rows: StaffMemberResponse[]
    mode: "restaurant" | "account"
    onEdit: (member: StaffMemberResponse) => void
    onRemove: (member: StaffMemberResponse) => void
    onAccountStatus: (member: StaffMemberResponse, status: "ACTIVE" | "SUSPENDED") => void
}) {
    return (
        <section className="overflow-hidden border">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Employee</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Assignments</TableHead>
                        <TableHead>Last login</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {rows.map((member) => (
                        <TableRow key={member.userId}>
                            <TableCell>
                                <p className="font-medium">{member.fullName || member.email}</p>
                                <p className="text-xs text-muted-foreground">{member.email}</p>
                            </TableCell>
                            <TableCell><StatusBadge status={member.accountStatus} /></TableCell>
                            <TableCell>
                                <div className="space-y-1">
                                    {member.assignments.map((assignment) => (
                                        <p key={assignment.restaurantId} className="text-xs">
                                            <span className="font-medium">{assignment.restaurantName}</span>
                                            <span className="text-muted-foreground"> - {assignment.roleName} - {assignment.assignmentStatus}</span>
                                        </p>
                                    ))}
                                </div>
                            </TableCell>
                            <TableCell>{member.lastLoginAt ? new Date(member.lastLoginAt).toLocaleString("es-CO") : "-"}</TableCell>
                            <TableCell className="text-right">
                                {mode === "restaurant" ? (
                                    <div className="flex justify-end gap-1">
                                        <Button size="icon" variant="ghost" title="Edit assignment" onClick={() => onEdit(member)}>
                                            <Edit className="h-4 w-4" />
                                        </Button>
                                        <Button size="icon" variant="ghost" title="Remove access" onClick={() => onRemove(member)}>
                                            <UserMinus className="h-4 w-4" />
                                        </Button>
                                    </div>
                                ) : (
                                    <Button
                                        size="icon"
                                        variant="ghost"
                                        title={member.accountStatus === "ACTIVE" ? "Suspend account" : "Reactivate account"}
                                        onClick={() => onAccountStatus(member, member.accountStatus === "ACTIVE" ? "SUSPENDED" : "ACTIVE")}
                                    >
                                        {member.accountStatus === "ACTIVE"
                                            ? <UserRoundX className="h-4 w-4" />
                                            : <UserRoundCheck className="h-4 w-4" />}
                                    </Button>
                                )}
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </section>
    )
}

function RolesTable({ rows }: { rows: StaffRoleResponse[] }) {
    return (
        <section className="overflow-hidden border">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Role</TableHead>
                        <TableHead>Preset</TableHead>
                        <TableHead>Users</TableHead>
                        <TableHead>Permissions</TableHead>
                        <TableHead>Scope</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {rows.map((role) => (
                        <TableRow key={role.id}>
                            <TableCell>
                                <p className="font-medium">{role.name}</p>
                                <StatusBadge status={role.active ? "ACTIVE" : "SUSPENDED"} />
                            </TableCell>
                            <TableCell>{role.presetKey ?? "System"}</TableCell>
                            <TableCell>{role.assignedUsers}</TableCell>
                            <TableCell className="max-w-[460px]">
                                <p className="truncate text-xs text-muted-foreground" title={permissionSummary(role.permissions)}>
                                    {permissionSummary(role.permissions)}
                                </p>
                            </TableCell>
                            <TableCell className="text-xs text-muted-foreground">Platform-defined</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </section>
    )
}

function InvitationsTable({
    rows, onCopy, onRegenerate, onRevoke,
}: {
    rows: StaffInvitationResponse[]
    onCopy: (path: string) => void
    onRegenerate: (id: string) => void
    onRevoke: (row: StaffInvitationResponse) => void
}) {
    return (
        <section className="overflow-hidden border">
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Email</TableHead>
                        <TableHead>Role</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Expires</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {rows.map((row) => (
                        <TableRow key={row.id}>
                            <TableCell className="font-medium">{row.email}</TableCell>
                            <TableCell>{row.roleName}</TableCell>
                            <TableCell><StatusBadge status={row.status} /></TableCell>
                            <TableCell>{new Date(row.expiresAt).toLocaleString("es-CO")}</TableCell>
                            <TableCell className="text-right">
                                {row.invitationPath && (
                                    <Button size="icon" variant="ghost" title="Copy link" onClick={() => onCopy(row.invitationPath!)}>
                                        <Copy className="h-4 w-4" />
                                    </Button>
                                )}
                                <Button size="sm" variant="ghost" disabled={row.status === "ACCEPTED" || row.status === "REVOKED"} onClick={() => onRegenerate(row.id)}>
                                    Regenerate
                                </Button>
                                <Button size="sm" variant="ghost" disabled={row.status !== "PENDING"} onClick={() => onRevoke(row)}>
                                    Revoke
                                </Button>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </section>
    )
}

function InviteDialog({
    open, roles, onOpenChange, onSubmit,
}: {
    open: boolean
    roles: StaffRoleResponse[]
    onOpenChange: (open: boolean) => void
    onSubmit: (payload: { email: string; roleId: string }) => Promise<void>
}) {
    const [email, setEmail] = useState("")
    const [roleId, setRoleId] = useState("")
    return (
        <DialogShell open={open} onOpenChange={onOpenChange} title="Invite employee" description="A shareable link will be generated and copied manually.">
            <div className="space-y-4">
                <div className="space-y-2">
                    <Label>Email</Label>
                    <Input type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
                </div>
                <div className="space-y-2">
                    <Label>Role</Label>
                    <Select value={roleId} onValueChange={setRoleId}>
                        <SelectTrigger><SelectValue placeholder="Select role" /></SelectTrigger>
                        <SelectContent>
                            {roles.map((role) => <SelectItem key={role.id} value={role.id}>{role.name}</SelectItem>)}
                        </SelectContent>
                    </Select>
                </div>
                <div className="flex justify-end gap-2">
                    <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
                    <Button disabled={!email || !roleId} onClick={() => onSubmit({ email, roleId })}>Create invitation</Button>
                </div>
            </div>
        </DialogShell>
    )
}

function AssignmentDialog({
    open, member, roles, onOpenChange, onSubmit,
}: {
    open: boolean
    member: StaffMemberResponse | null
    roles: StaffRoleResponse[]
    onOpenChange: (open: boolean) => void
    onSubmit: (userId: string, payload: { roleId?: string; status: "ACTIVE" | "SUSPENDED" }) => Promise<void>
}) {
    const assignment = member?.assignments[0]
    const [roleId, setRoleId] = useState(assignment?.roleId ?? "")
    const [status, setStatus] = useState<"ACTIVE" | "SUSPENDED">(
        assignment?.assignmentStatus === "SUSPENDED" ? "SUSPENDED" : "ACTIVE"
    )
    return (
        <DialogShell open={open} onOpenChange={onOpenChange} title="Edit employee access">
            <div className="space-y-4">
                <p className="text-sm text-muted-foreground">{member?.email}</p>
                <div className="space-y-2">
                    <Label>Role</Label>
                    <Select value={roleId} onValueChange={setRoleId}>
                        <SelectTrigger><SelectValue placeholder="Select role" /></SelectTrigger>
                        <SelectContent>{roles.map((role) => <SelectItem key={role.id} value={role.id}>{role.name}</SelectItem>)}</SelectContent>
                    </Select>
                </div>
                <div className="space-y-2">
                    <Label>Status</Label>
                    <Select value={status} onValueChange={(value) => setStatus(value as "ACTIVE" | "SUSPENDED")}>
                        <SelectTrigger><SelectValue /></SelectTrigger>
                        <SelectContent>
                            <SelectItem value="ACTIVE">Active</SelectItem>
                            <SelectItem value="SUSPENDED">Suspended for this restaurant</SelectItem>
                        </SelectContent>
                    </Select>
                </div>
                <div className="flex justify-end gap-2">
                    <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
                    <Button disabled={!member || !roleId} onClick={() => member && onSubmit(member.userId, { roleId, status })}>Save</Button>
                </div>
            </div>
        </DialogShell>
    )
}

function StatusBadge({ status }: { status: string }) {
    const variant: "default" | "outline" | "secondary" =
        status === "ACTIVE" || status === "PENDING"
            ? "default"
            : status === "ACCEPTED"
                ? "outline"
                : "secondary"
    return <Badge variant={variant}>{status}</Badge>
}

function permissionSummary(permissions: StaffRoleResponse["permissions"]) {
    return Object.entries(permissions)
        .map(([module, actions]) => `${module}: ${(actions ?? []).join(", ")}`)
        .join(" - ")
}

function confirmationText(confirm: NonNullable<ConfirmState>) {
    if (confirm.type === "remove-assignment") return `Remove restaurant access for ${confirm.label}? Historical records will be preserved.`
    if (confirm.type === "account-status") return `${confirm.status === "SUSPENDED" ? "Suspend" : "Reactivate"} account access for ${confirm.label}?`
    return `Revoke invitation for ${confirm.label}?`
}

