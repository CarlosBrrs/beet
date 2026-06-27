"use client"

import { ChevronLeft, ChevronRight, Eye, Loader2, ShieldAlert } from "lucide-react"
import { useState } from "react"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { Can } from "@/components/shared/can"
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
import { CashSessionListResponse, RestaurantResponse } from "@/lib/api-types"
import { formatCurrency } from "@/lib/formatters"
import { useCashRegisters } from "@/lib/hooks/use-cash-registers"
import { useCashSessionList } from "@/lib/hooks/use-cash-sessions"
import { useMyRestaurants } from "@/lib/hooks/use-my-restaurants"

import { ForceCloseCashSessionDialog } from "./force-close-cash-session-dialog"
import { ReconciliationDialog } from "./reconciliation-dialog"

const ALL = "ALL"

function currentDate() {
    return new Date().toLocaleDateString("en-CA")
}

function formatDateTime(value: string | null) {
    if (!value) return "-"
    return new Intl.DateTimeFormat("es-CO", {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(new Date(value))
}

interface CashSessionManagementProps {
    scope: "restaurant" | "account"
    restaurantId?: string
    restaurants?: RestaurantResponse[]
    registers?: { id: string; name: string }[]
}

export function RestaurantCashSessionManagement() {
    const { restaurantId } = useRestaurantContext()
    const { data: registers = [] } = useCashRegisters()
    return (
        <CashSessionManagement
            scope="restaurant"
            restaurantId={restaurantId ?? undefined}
            registers={registers}
        />
    )
}

export function AccountCashSessionManagement() {
    const { data: restaurants = [] } = useMyRestaurants()
    return <CashSessionManagement scope="account" restaurants={restaurants} />
}

function CashSessionManagement({
    scope,
    restaurantId,
    restaurants = [],
    registers = [],
}: CashSessionManagementProps) {
    const [tab, setTab] = useState<"open" | "history">("open")
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(20)
    const [from, setFrom] = useState(currentDate)
    const [to, setTo] = useState(currentDate)
    const [restaurantFilter, setRestaurantFilter] = useState(ALL)
    const [registerFilter, setRegisterFilter] = useState(ALL)
    const [forceCloseTarget, setForceCloseTarget] = useState<CashSessionListResponse | null>(null)
    const [reconciliationTarget, setReconciliationTarget] = useState<CashSessionListResponse | null>(null)

    const { data, isLoading, isError, error } = useCashSessionList({
        scope,
        restaurantId: scope === "account"
            ? (restaurantFilter === ALL ? undefined : restaurantFilter)
            : restaurantId,
        cashRegisterId: registerFilter === ALL ? undefined : registerFilter,
        status: tab === "open" ? "OPEN" : "CLOSED",
        from: tab === "history" ? from : undefined,
        to: tab === "history" ? to : undefined,
        page,
        size,
    })

    const sessions = data?.content ?? []
    const totalPages = data?.totalPages ?? 0

    const changeTab = (value: string) => {
        setTab(value as "open" | "history")
        setPage(0)
    }

    return (
        <>
            <section className="space-y-4">
                <div>
                    <h2 className="text-lg font-semibold">Cash Sessions</h2>
                </div>
                <Tabs value={tab} onValueChange={changeTab}>
                    <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
                        <TabsList>
                            <TabsTrigger value="open">Open Sessions</TabsTrigger>
                            <TabsTrigger value="history">History</TabsTrigger>
                        </TabsList>
                        <div className="flex flex-wrap items-end gap-2">
                            {scope === "account" && (
                                <FilterSelect
                                    label="Restaurant"
                                    value={restaurantFilter}
                                    onValueChange={(value) => {
                                        setRestaurantFilter(value)
                                        setPage(0)
                                    }}
                                    options={restaurants.map((restaurant) => ({
                                        value: restaurant.id,
                                        label: restaurant.name,
                                    }))}
                                />
                            )}
                            {scope === "restaurant" && (
                                <FilterSelect
                                    label="Cash register"
                                    value={registerFilter}
                                    onValueChange={(value) => {
                                        setRegisterFilter(value)
                                        setPage(0)
                                    }}
                                    options={registers.map((register) => ({
                                        value: register.id,
                                        label: register.name,
                                    }))}
                                />
                            )}
                            {tab === "history" && (
                                <>
                                    <DateFilter label="From" value={from} onChange={(value) => {
                                        setFrom(value)
                                        setPage(0)
                                    }} />
                                    <DateFilter label="To" value={to} onChange={(value) => {
                                        setTo(value)
                                        setPage(0)
                                    }} />
                                </>
                            )}
                        </div>
                    </div>
                    <TabsContent value={tab}>
                        {isLoading ? (
                            <div className="flex h-32 items-center justify-center border">
                                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                            </div>
                        ) : isError ? (
                            <div className="border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
                                {error.message || "Failed to load cash sessions"}
                            </div>
                        ) : (
                            <SessionTable
                                sessions={sessions}
                                showRestaurant={scope === "account"}
                                showForceClose={tab === "open"}
                                onForceClose={setForceCloseTarget}
                                onViewReconciliation={setReconciliationTarget}
                            />
                        )}
                    </TabsContent>
                </Tabs>
                <div className="flex flex-col gap-3 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
                    <span>{data?.totalElements ?? 0} session(s)</span>
                    <div className="flex items-center gap-2">
                        <Select value={String(size)} onValueChange={(value) => {
                            setSize(Number(value))
                            setPage(0)
                        }}>
                            <SelectTrigger className="w-[72px]">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {[10, 20, 50, 100].map((option) => (
                                    <SelectItem key={option} value={String(option)}>{option}</SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <span>Page {totalPages === 0 ? 0 : page + 1} of {totalPages}</span>
                        <Button
                            variant="outline"
                            size="icon"
                            title="Previous page"
                            disabled={page === 0}
                            onClick={() => setPage((current) => Math.max(current - 1, 0))}
                        >
                            <ChevronLeft className="h-4 w-4" />
                        </Button>
                        <Button
                            variant="outline"
                            size="icon"
                            title="Next page"
                            disabled={page + 1 >= totalPages}
                            onClick={() => setPage((current) => current + 1)}
                        >
                            <ChevronRight className="h-4 w-4" />
                        </Button>
                    </div>
                </div>
            </section>

            <ForceCloseCashSessionDialog
                session={forceCloseTarget}
                open={!!forceCloseTarget}
                onOpenChange={(open) => {
                    if (!open) setForceCloseTarget(null)
                }}
            />
            <ReconciliationDialog
                session={reconciliationTarget}
                open={!!reconciliationTarget}
                onOpenChange={(open) => {
                    if (!open) setReconciliationTarget(null)
                }}
            />
        </>
    )
}

function SessionTable({
    sessions,
    showRestaurant,
    showForceClose,
    onForceClose,
    onViewReconciliation,
}: {
    sessions: CashSessionListResponse[]
    showRestaurant: boolean
    showForceClose: boolean
    onForceClose: (session: CashSessionListResponse) => void
    onViewReconciliation: (session: CashSessionListResponse) => void
}) {
    const columnCount = 9 + Number(showRestaurant)

    return (
        <div className="border">
            <Table>
                <TableHeader>
                    <TableRow>
                        {showRestaurant && <TableHead>Restaurant</TableHead>}
                        <TableHead>Cash register</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Opened</TableHead>
                        <TableHead>Opening amount</TableHead>
                        <TableHead>Closed</TableHead>
                        <TableHead>Closing amount</TableHead>
                        <TableHead>Expected</TableHead>
                        <TableHead>Difference</TableHead>
                        <TableHead />
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {sessions.length === 0 ? (
                        <TableRow>
                            <TableCell colSpan={columnCount} className="h-24 text-center">
                                No sessions found.
                            </TableCell>
                        </TableRow>
                    ) : sessions.map((session) => (
                        <TableRow key={session.id}>
                            {showRestaurant && <TableCell>{session.restaurantName}</TableCell>}
                            <TableCell>{session.cashRegisterName}</TableCell>
                            <TableCell>
                                <Badge
                                    variant={session.status === "OPEN" ? "outline" : "secondary"}
                                    className={session.status === "OPEN" ? "border-green-300 bg-green-50 text-green-700" : undefined}
                                >
                                    {session.status === "OPEN" ? "Open" : "Closed"}
                                </Badge>
                            </TableCell>
                            <TableCell>{formatDateTime(session.openedAt)}</TableCell>
                            <TableCell>{formatCurrency(Number(session.openingAmount))}</TableCell>
                            <TableCell>{formatDateTime(session.closedAt)}</TableCell>
                            <TableCell>
                                {session.closingAmount === null ? "-" : formatCurrency(Number(session.closingAmount))}
                            </TableCell>
                            <TableCell>
                                {session.expectedCash === null ? "-" : formatCurrency(Number(session.expectedCash))}
                            </TableCell>
                            <TableCell>
                                {session.differenceAmount === null
                                    ? "-"
                                    : (
                                        <div>
                                            <span className={Number(session.differenceAmount) === 0 ? "" : "text-destructive"}>
                                                {formatCurrency(Number(session.differenceAmount))}
                                            </span>
                                            {session.differenceReason && (
                                                <p className="max-w-48 truncate text-xs text-muted-foreground">
                                                    {session.differenceReason}
                                                </p>
                                            )}
                                        </div>
                                    )}
                            </TableCell>
                            <TableCell className="text-right">
                                <div className="flex justify-end gap-1">
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        title="Ver arqueo"
                                        onClick={() => onViewReconciliation(session)}
                                    >
                                        <Eye className="h-4 w-4" />
                                    </Button>
                                    {showForceClose && (
                                    <Can I="MANAGE" a="CASH" restaurantId={session.restaurantId}>
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            title="Force close session"
                                            onClick={() => onForceClose(session)}
                                        >
                                            <ShieldAlert className="h-4 w-4" />
                                        </Button>
                                    </Can>
                                    )}
                                </div>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </div>
    )
}

function FilterSelect({
    label,
    value,
    options,
    onValueChange,
}: {
    label: string
    value: string
    options: { value: string; label: string }[]
    onValueChange: (value: string) => void
}) {
    return (
        <label className="space-y-1 text-xs">
            <span className="block text-muted-foreground">{label}</span>
            <Select value={value} onValueChange={onValueChange}>
                <SelectTrigger className="w-[180px]">
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value={ALL}>All</SelectItem>
                    {options.map((option) => (
                        <SelectItem key={option.value} value={option.value}>{option.label}</SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </label>
    )
}

function DateFilter({
    label,
    value,
    onChange,
}: {
    label: string
    value: string
    onChange: (value: string) => void
}) {
    return (
        <label className="space-y-1 text-xs">
            <span className="block text-muted-foreground">{label}</span>
            <Input type="date" value={value} onChange={(event) => onChange(event.target.value)} />
        </label>
    )
}
