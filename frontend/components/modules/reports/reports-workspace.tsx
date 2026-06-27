"use client"

import Link from "next/link"
import { usePathname, useRouter, useSearchParams } from "next/navigation"
import { Eye, Search } from "lucide-react"
import { useEffect, useMemo, useState } from "react"
import type { ReactNode } from "react"

import { useOptionalRestaurantContext } from "@/components/providers/restaurant-provider"
import { DialogShell } from "@/components/shared/dialog-shell"
import { ListPagination } from "@/components/shared/list-pagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
    Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { formatCurrency, formatNumber } from "@/lib/formatters"
import { useDebounce } from "@/lib/hooks/use-debounce"
import { useMyRestaurants } from "@/lib/hooks/use-my-restaurants"
import {
    ReportFilters,
    ReportGrouping,
    useBusinessDayDetailReport,
    useBusinessDayReport,
    useInventoryConsumptionReport,
    useInventoryValuationReport,
    useLowStockReport,
    usePaymentMethodReport,
    useProductReport,
    useReportOverview,
    useSalesSeries,
    useTemplateReport,
} from "@/lib/hooks/use-reports"
import { ReportChart } from "./report-chart"
import { ReportFilters as ReportFilterBar } from "./report-filters"
import { ReportKpis } from "./report-kpis"

type ReportTab = "overview" | "sales" | "cash" | "products" | "templates" | "inventory"
type PagedReport = "payments" | "businessDays" | "products" | "templates" | "consumption" | "lowStock"

const INITIAL_PAGES: Record<PagedReport, number> = {
    payments: 0,
    businessDays: 0,
    products: 0,
    templates: 0,
    consumption: 0,
    lowStock: 0,
}

function isoDate(date: Date) {
    return date.toISOString().slice(0, 10)
}

export function ReportsWorkspace() {
    const restaurant = useOptionalRestaurantContext()
    const isAccount = !restaurant?.restaurantId
    const router = useRouter()
    const pathname = usePathname()
    const searchParams = useSearchParams()
    const today = useMemo(() => new Date(), [])
    const thirtyDaysAgo = useMemo(() => {
        const date = new Date(today)
        date.setDate(date.getDate() - 29)
        return date
    }, [today])

    const [tab, setTab] = useState<ReportTab>((searchParams.get("tab") as ReportTab) || "overview")
    const [dateFrom, setDateFrom] = useState(searchParams.get("dateFrom") || isoDate(thirtyDaysAgo))
    const [dateTo, setDateTo] = useState(searchParams.get("dateTo") || isoDate(today))
    const [grouping, setGrouping] = useState<ReportGrouping>(
        (searchParams.get("grouping") as ReportGrouping) || "DAY"
    )
    const [selectedRestaurantIds, setSelectedRestaurantIds] = useState<string[]>(
        searchParams.getAll("restaurantId")
    )
    const [search, setSearch] = useState("")
    const [pages, setPages] = useState(INITIAL_PAGES)
    const [size, setSize] = useState(20)
    const [selectedBusinessDay, setSelectedBusinessDay] = useState<string | null>(
        searchParams.get("businessDay")
    )
    const debouncedSearch = useDebounce(search, 300)
    const { data: restaurants = [] } = useMyRestaurants()

    useEffect(() => {
        const params = new URLSearchParams()
        params.set("tab", tab)
        params.set("dateFrom", dateFrom)
        params.set("dateTo", dateTo)
        params.set("grouping", grouping)
        selectedRestaurantIds.forEach((id) => params.append("restaurantId", id))
        if (selectedBusinessDay) params.set("businessDay", selectedBusinessDay)
        router.replace(`${pathname}?${params.toString()}`, { scroll: false })
    }, [
        dateFrom, dateTo, grouping, pathname, router, selectedBusinessDay,
        selectedRestaurantIds, tab,
    ])

    const filters: ReportFilters = {
        dateFrom,
        dateTo,
        grouping,
        restaurantIds: isAccount && selectedRestaurantIds.length > 0 ? selectedRestaurantIds : undefined,
        size,
        search: debouncedSearch || undefined,
    }

    const overview = useReportOverview(filters)
    const series = useSalesSeries(filters, tab === "overview" || tab === "sales")
    const payments = usePaymentMethodReport({ ...filters, page: pages.payments }, tab === "cash")
    const businessDays = useBusinessDayReport({ ...filters, page: pages.businessDays }, tab === "cash")
    const products = useProductReport({ ...filters, page: pages.products }, tab === "products")
    const templates = useTemplateReport({ ...filters, page: pages.templates }, tab === "templates")
    const consumption = useInventoryConsumptionReport(
        { ...filters, page: pages.consumption },
        tab === "inventory"
    )
    const valuation = useInventoryValuationReport(filters, tab === "inventory")
    const lowStock = useLowStockReport({ ...filters, page: pages.lowStock }, tab === "inventory")
    const businessDayDetail = useBusinessDayDetailReport(selectedBusinessDay, !!selectedBusinessDay)

    const resetPages = () => setPages(INITIAL_PAGES)
    const setReportPage = (report: PagedReport, page: number) => {
        setPages((current) => ({ ...current, [report]: page }))
    }

    const updateTab = (value: string) => {
        setTab(value as ReportTab)
        setSearch("")
        setSelectedBusinessDay(null)
    }

    return (
        <div className="space-y-6 pb-12">
            <div>
                <h1 className="text-3xl font-bold tracking-tight">
                    {isAccount ? "Reportes de la cuenta" : "Reportes"}
                </h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    {isAccount
                        ? "Consolidado de los restaurantes a los que tienes acceso."
                        : "Ventas, caja, catalogo e inventario del restaurante."}
                </p>
            </div>

            <ReportFilterBar
                dateFrom={dateFrom}
                dateTo={dateTo}
                grouping={grouping}
                restaurants={isAccount ? restaurants : undefined}
                selectedRestaurantIds={selectedRestaurantIds}
                onDateFromChange={(value) => { setDateFrom(value); resetPages() }}
                onDateToChange={(value) => { setDateTo(value); resetPages() }}
                onGroupingChange={setGrouping}
                onRestaurantSelectionChange={(ids) => { setSelectedRestaurantIds(ids); resetPages() }}
            />

            {overview.isLoading ? (
                <div className="h-28 animate-pulse border bg-muted/30" />
            ) : overview.data ? (
                <ReportKpis overview={overview.data} />
            ) : (
                <div className="border p-6 text-sm text-destructive">No fue posible cargar el resumen.</div>
            )}

            <Tabs value={tab} onValueChange={updateTab}>
                <TabsList variant="line" className="max-w-full overflow-x-auto">
                    <TabsTrigger value="overview">Resumen</TabsTrigger>
                    <TabsTrigger value="sales">Ventas</TabsTrigger>
                    <TabsTrigger value="cash">Cajas y pagos</TabsTrigger>
                    <TabsTrigger value="products">Productos</TabsTrigger>
                    <TabsTrigger value="templates">Armables</TabsTrigger>
                    <TabsTrigger value="inventory">Inventario</TabsTrigger>
                </TabsList>

                <TabsContent value="overview" className="space-y-6 pt-4">
                    <SectionTitle title="Tendencia" provisional={series.data?.provisional} />
                    <ReportChart points={series.data?.points ?? []} />
                    <ServiceTypeTable serviceTypes={overview.data?.serviceTypes ?? []} />
                    {(overview.data?.restaurants.length ?? 0) > 1 && (
                        <RestaurantComparison restaurants={overview.data?.restaurants ?? []} />
                    )}
                </TabsContent>

                <TabsContent value="sales" className="space-y-6 pt-4">
                    <SectionTitle title="Ventas en el tiempo" provisional={series.data?.provisional} />
                    <ReportChart points={series.data?.points ?? []} />
                    <SalesTable points={series.data?.points ?? []} />
                </TabsContent>

                <TabsContent value="cash" className="space-y-8 pt-4">
                    <SearchBox value={search} onChange={(value) => { setSearch(value); resetPages() }} />
                    <PaymentMethodsTable
                        data={payments.data}
                        page={pages.payments}
                        size={size}
                        onPageChange={(page) => setReportPage("payments", page)}
                        onSizeChange={(value) => { setSize(value); resetPages() }}
                    />
                    <BusinessDaysTable
                        data={businessDays.data}
                        isAccount={isAccount}
                        onView={setSelectedBusinessDay}
                        page={pages.businessDays}
                        size={size}
                        onPageChange={(page) => setReportPage("businessDays", page)}
                        onSizeChange={(value) => { setSize(value); resetPages() }}
                    />
                </TabsContent>

                <TabsContent value="products" className="space-y-4 pt-4">
                    <SearchBox value={search} onChange={(value) => { setSearch(value); resetPages() }} />
                    <CatalogTable
                        title="Productos"
                        data={products.data}
                        page={pages.products}
                        size={size}
                        onPageChange={(page) => setReportPage("products", page)}
                        onSizeChange={(value) => { setSize(value); resetPages() }}
                    />
                </TabsContent>

                <TabsContent value="templates" className="space-y-4 pt-4">
                    <SearchBox value={search} onChange={(value) => { setSearch(value); resetPages() }} />
                    <CatalogTable
                        title="Armables"
                        data={templates.data}
                        page={pages.templates}
                        size={size}
                        onPageChange={(page) => setReportPage("templates", page)}
                        onSizeChange={(value) => { setSize(value); resetPages() }}
                    />
                </TabsContent>

                <TabsContent value="inventory" className="space-y-8 pt-4">
                    <InventoryValuation data={valuation.data} />
                    <SearchBox value={search} onChange={(value) => { setSearch(value); resetPages() }} />
                    <InventoryTables
                        consumption={consumption.data}
                        lowStock={lowStock.data}
                        consumptionPage={pages.consumption}
                        lowStockPage={pages.lowStock}
                        size={size}
                        onConsumptionPageChange={(page) => setReportPage("consumption", page)}
                        onLowStockPageChange={(page) => setReportPage("lowStock", page)}
                        onSizeChange={(value) => { setSize(value); resetPages() }}
                    />
                </TabsContent>
            </Tabs>

            {!isAccount && (
                <BusinessDayDetailDialog
                    open={!!selectedBusinessDay}
                    onOpenChange={(open) => !open && setSelectedBusinessDay(null)}
                    data={businessDayDetail.data}
                />
            )}
        </div>
    )
}

function SectionTitle({ title, provisional }: { title: string; provisional?: boolean }) {
    return (
        <div className="flex items-center gap-2">
            <h2 className="text-sm font-semibold">{title}</h2>
            {provisional && <Badge variant="secondary">Provisional</Badge>}
        </div>
    )
}

function SearchBox({ value, onChange }: { value: string; onChange: (value: string) => void }) {
    return (
        <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input value={value} onChange={(event) => onChange(event.target.value)} placeholder="Buscar..." className="pl-9" />
        </div>
    )
}

function ServiceTypeTable({ serviceTypes }: {
    serviceTypes: Array<{ serviceType: string; orderCount: number; grossSales: number }>
}) {
    return (
        <ReportTable title="Ventas por tipo de servicio" headers={["Servicio", "Ordenes", "Ventas"]}>
            {serviceTypes.map((row) => (
                <TableRow key={row.serviceType}>
                    <TableCell>{row.serviceType}</TableCell>
                    <TableCell>{row.orderCount}</TableCell>
                    <TableCell className="text-right">{formatCurrency(row.grossSales)}</TableCell>
                </TableRow>
            ))}
        </ReportTable>
    )
}

function RestaurantComparison({ restaurants }: {
    restaurants: Array<{
        restaurantId: string
        restaurantName: string
        grossSales: number
        collected: number
        completedOrders: number
    }>
}) {
    return (
        <ReportTable title="Comparacion por restaurante" headers={["Restaurante", "Ventas", "Cobrado", "Ordenes completadas"]}>
            {restaurants.map((row) => (
                <TableRow key={row.restaurantId}>
                    <TableCell className="font-medium">{row.restaurantName}</TableCell>
                    <TableCell className="text-right">{formatCurrency(row.grossSales)}</TableCell>
                    <TableCell className="text-right">{formatCurrency(row.collected)}</TableCell>
                    <TableCell className="text-right">{row.completedOrders}</TableCell>
                </TableRow>
            ))}
        </ReportTable>
    )
}

function SalesTable({ points }: { points: Array<{
    period: string; grossSales: number; completedSales: number; collected: number; refunds: number; orderCount: number
}> }) {
    return (
        <ReportTable title="Detalle por periodo" headers={["Periodo", "Ventas", "Completadas", "Cobrado", "Devoluciones", "Ordenes"]}>
            {points.map((row) => (
                <TableRow key={row.period}>
                    <TableCell>{row.period}</TableCell>
                    <TableCell className="text-right">{formatCurrency(row.grossSales)}</TableCell>
                    <TableCell className="text-right">{formatCurrency(row.completedSales)}</TableCell>
                    <TableCell className="text-right">{formatCurrency(row.collected)}</TableCell>
                    <TableCell className="text-right">{formatCurrency(row.refunds)}</TableCell>
                    <TableCell className="text-right">{row.orderCount}</TableCell>
                </TableRow>
            ))}
        </ReportTable>
    )
}

function PaymentMethodsTable({
    data, page, size, onPageChange, onSizeChange,
}: {
    data: ReturnType<typeof usePaymentMethodReport>["data"]
    page: number
    size: number
    onPageChange: (page: number) => void
    onSizeChange: (size: number) => void
}) {
    return (
        <div className="space-y-3">
            <ReportTable title="Metodos de pago" headers={["Metodo", "Restaurante", "Pagos", "Cobrado", "Propinas", "Devoluciones", "Neto"]}>
                {(data?.content ?? []).map((row) => (
                    <TableRow key={`${row.restaurantId}-${row.paymentMethodId}`}>
                        <TableCell><span className="font-medium">{row.methodName}</span><span className="ml-2 text-muted-foreground">{row.methodCode}</span></TableCell>
                        <TableCell>{row.restaurantName}</TableCell>
                        <TableCell>{row.paymentCount}</TableCell>
                        <TableCell className="text-right">{formatCurrency(row.collected)}</TableCell>
                        <TableCell className="text-right">{formatCurrency(row.tips)}</TableCell>
                        <TableCell className="text-right">{formatCurrency(row.refunds)}</TableCell>
                        <TableCell className="text-right font-medium">{formatCurrency(row.netCollected)}</TableCell>
                    </TableRow>
                ))}
            </ReportTable>
            <ListPagination
                totalElements={data?.totalElements ?? 0}
                totalPages={data?.totalPages ?? 0}
                page={page}
                size={size}
                label="metodos"
                onPageChange={onPageChange}
                onSizeChange={onSizeChange}
            />
        </div>
    )
}

function BusinessDaysTable({
    data, isAccount, onView, page, size, onPageChange, onSizeChange,
}: {
    data: ReturnType<typeof useBusinessDayReport>["data"]
    isAccount: boolean
    onView: (id: string) => void
    page: number
    size: number
    onPageChange: (page: number) => void
    onSizeChange: (size: number) => void
}) {
    return (
        <div className="space-y-3">
            <ReportTable title="Dias operativos" headers={["Fecha", "Restaurante", "Estado", "Cobrado", "Esperado", "Contado", "Diferencia", ""]}>
                {(data?.content ?? []).map((row) => (
                    <TableRow key={row.id}>
                        <TableCell>{row.businessDate}</TableCell>
                        <TableCell>{row.restaurantName}</TableCell>
                        <TableCell>
                            <Badge variant={row.provisional ? "secondary" : "outline"}>
                                {row.provisional ? "Provisional" : `Cierre ${row.closureSequence}`}
                            </Badge>
                        </TableCell>
                        <TableCell className="text-right">{formatCurrency(row.paymentsTotal)}</TableCell>
                        <TableCell className="text-right">{row.expectedCash == null ? "-" : formatCurrency(row.expectedCash)}</TableCell>
                        <TableCell className="text-right">{row.countedCash == null ? "-" : formatCurrency(row.countedCash)}</TableCell>
                        <TableCell className="text-right">{row.difference == null ? "-" : formatCurrency(row.difference)}</TableCell>
                        <TableCell className="text-right">
                            {isAccount ? (
                                <Button variant="ghost" size="icon" title="Ver en restaurante" asChild>
                                    <Link href={`/restaurants/${row.restaurantId}/reports?tab=cash&businessDay=${row.id}`}>
                                        <Eye className="h-4 w-4" />
                                    </Link>
                                </Button>
                            ) : (
                                <Button variant="ghost" size="icon" title="Ver detalle" onClick={() => onView(row.id)}>
                                    <Eye className="h-4 w-4" />
                                </Button>
                            )}
                        </TableCell>
                    </TableRow>
                ))}
            </ReportTable>
            <ListPagination
                totalElements={data?.totalElements ?? 0}
                totalPages={data?.totalPages ?? 0}
                page={page}
                size={size}
                label="dias"
                onPageChange={onPageChange}
                onSizeChange={onSizeChange}
            />
        </div>
    )
}

function CatalogTable({
    title, data, page, size, onPageChange, onSizeChange,
}: {
    title: string
    data: ReturnType<typeof useProductReport>["data"]
    page: number
    size: number
    onPageChange: (page: number) => void
    onSizeChange: (size: number) => void
}) {
    return (
        <div className="space-y-3">
            <ReportTable title={title} headers={["Nombre", "Restaurante", "Vendidas", "Canceladas", "Ventas", "Costo", "Margen"]}>
                {(data?.content ?? []).map((row) => (
                    <TableRow key={`${row.restaurantId}-${row.referenceId}`}>
                        <TableCell>
                            <p className="font-medium">{row.name}</p>
                            {row.optionBreakdown && (
                                <p className="max-w-[360px] truncate text-xs text-muted-foreground" title={row.optionBreakdown}>
                                    {row.optionBreakdown}
                                </p>
                            )}
                        </TableCell>
                        <TableCell>{row.restaurantName}</TableCell>
                        <TableCell>{formatNumber(row.soldQuantity)}</TableCell>
                        <TableCell>{formatNumber(row.canceledQuantity)}</TableCell>
                        <TableCell className="text-right">{formatCurrency(row.grossSales)}</TableCell>
                        <TableCell className="text-right">
                            {row.costComplete && row.theoreticalCost != null
                                ? formatCurrency(row.theoreticalCost)
                                : <Badge variant="secondary">Costo incompleto</Badge>}
                        </TableCell>
                        <TableCell className="text-right">
                            {row.theoreticalMargin == null ? "-" : formatCurrency(row.theoreticalMargin)}
                        </TableCell>
                    </TableRow>
                ))}
            </ReportTable>
            <ListPagination
                totalElements={data?.totalElements ?? 0}
                totalPages={data?.totalPages ?? 0}
                page={page}
                size={size}
                label={title.toLowerCase()}
                onPageChange={onPageChange}
                onSizeChange={onSizeChange}
            />
        </div>
    )
}

function InventoryValuation({ data }: { data: ReturnType<typeof useInventoryValuationReport>["data"] }) {
    return (
        <section>
            <h2 className="mb-3 text-sm font-semibold">Valoracion actual</h2>
            <div className="grid border sm:grid-cols-3">
                <Metric label="Valor conocido" value={formatCurrency(data?.knownValue ?? 0)} />
                <Metric label="Ingredientes valorados" value={String(data?.valuedIngredientCount ?? 0)} />
                <Metric label="Sin costo activo" value={String(data?.missingCostIngredientCount ?? 0)} />
            </div>
            {(data?.missingCostIngredients.length ?? 0) > 0 && (
                <p className="mt-2 text-xs text-muted-foreground">
                    El valor conocido excluye ingredientes sin proveedor activo o costo vigente.
                </p>
            )}
        </section>
    )
}

function InventoryTables({
    consumption, lowStock, consumptionPage, lowStockPage, size,
    onConsumptionPageChange, onLowStockPageChange, onSizeChange,
}: {
    consumption: ReturnType<typeof useInventoryConsumptionReport>["data"]
    lowStock: ReturnType<typeof useLowStockReport>["data"]
    consumptionPage: number
    lowStockPage: number
    size: number
    onConsumptionPageChange: (page: number) => void
    onLowStockPageChange: (page: number) => void
    onSizeChange: (size: number) => void
}) {
    return (
        <>
            <ReportTable title="Consumo por ventas" headers={["Ingrediente", "Restaurante", "Cantidad base", "Costo historico"]}>
                {(consumption?.content ?? []).map((row) => (
                    <TableRow key={`${row.restaurantId}-${row.ingredientId}`}>
                        <TableCell className="font-medium">{row.ingredientName}</TableCell>
                        <TableCell>{row.restaurantName}</TableCell>
                        <TableCell>{formatNumber(row.quantityBase, 6)}</TableCell>
                        <TableCell className="text-right">
                            {row.costComplete && row.historicalCost != null
                                ? formatCurrency(row.historicalCost)
                                : <Badge variant="secondary">Incompleto</Badge>}
                        </TableCell>
                    </TableRow>
                ))}
            </ReportTable>
            <ListPagination
                totalElements={consumption?.totalElements ?? 0}
                totalPages={consumption?.totalPages ?? 0}
                page={consumptionPage}
                size={size}
                label="ingredientes consumidos"
                onPageChange={onConsumptionPageChange}
                onSizeChange={onSizeChange}
            />
            <ReportTable title="Stock bajo" headers={["Ingrediente", "Restaurante", "Actual", "Minimo", "Faltante"]}>
                {(lowStock?.content ?? []).map((row) => (
                    <TableRow key={`${row.restaurantId}-${row.ingredientId}`}>
                        <TableCell className="font-medium">{row.ingredientName}</TableCell>
                        <TableCell>{row.restaurantName}</TableCell>
                        <TableCell>{formatNumber(row.currentStock)}</TableCell>
                        <TableCell>{formatNumber(row.minStock)}</TableCell>
                        <TableCell className="text-destructive">{formatNumber(row.shortage)}</TableCell>
                    </TableRow>
                ))}
            </ReportTable>
            <ListPagination
                totalElements={lowStock?.totalElements ?? 0}
                totalPages={lowStock?.totalPages ?? 0}
                page={lowStockPage}
                size={size}
                label="ingredientes con stock bajo"
                onPageChange={onLowStockPageChange}
                onSizeChange={onSizeChange}
            />
        </>
    )
}

function BusinessDayDetailDialog({
    open, onOpenChange, data,
}: {
    open: boolean
    onOpenChange: (open: boolean) => void
    data: ReturnType<typeof useBusinessDayDetailReport>["data"]
}) {
    return (
        <DialogShell
            open={open}
            onOpenChange={onOpenChange}
            title={data ? `Dia operativo ${data.businessDate}` : "Dia operativo"}
            description="Eventos, cierres conservados y arqueos de las sesiones."
            size="xl"
        >
            {!data ? (
                <div className="h-32 animate-pulse bg-muted/40" />
            ) : (
                <div className="max-h-[70vh] space-y-6 overflow-y-auto pr-1">
                    <ReportTable title="Cierres" headers={["Secuencia", "Cobrado", "Esperado", "Contado", "Diferencia", "Fecha"]}>
                        {data.closures.map((closure) => (
                            <TableRow key={closure.id}>
                                <TableCell>{closure.sequence}</TableCell>
                                <TableCell>{formatCurrency(closure.payments)}</TableCell>
                                <TableCell>{formatCurrency(closure.expectedCash)}</TableCell>
                                <TableCell>{formatCurrency(closure.countedCash)}</TableCell>
                                <TableCell>{formatCurrency(closure.difference)}</TableCell>
                                <TableCell>{new Date(closure.closedAt).toLocaleString("es-CO")}</TableCell>
                            </TableRow>
                        ))}
                    </ReportTable>
                    <ReportTable title="Sesiones de caja" headers={["Caja", "Estado", "Apertura", "Esperado", "Contado", "Diferencia"]}>
                        {data.sessions.map((session) => (
                            <TableRow key={session.id}>
                                <TableCell>{session.registerName}</TableCell>
                                <TableCell><Badge variant="outline">{session.status}</Badge></TableCell>
                                <TableCell>{formatCurrency(session.openingAmount)}</TableCell>
                                <TableCell>{session.expectedCash == null ? "-" : formatCurrency(session.expectedCash)}</TableCell>
                                <TableCell>{session.countedCash == null ? "-" : formatCurrency(session.countedCash)}</TableCell>
                                <TableCell>{session.difference == null ? "-" : formatCurrency(session.difference)}</TableCell>
                            </TableRow>
                        ))}
                    </ReportTable>
                    <ReportTable title="Eventos" headers={["Tipo", "Fecha", "Motivo"]}>
                        {data.events.map((event) => (
                            <TableRow key={`${event.type}-${event.occurredAt}`}>
                                <TableCell><Badge variant="secondary">{event.type}</Badge></TableCell>
                                <TableCell>{new Date(event.occurredAt).toLocaleString("es-CO")}</TableCell>
                                <TableCell>{event.reason || "-"}</TableCell>
                            </TableRow>
                        ))}
                    </ReportTable>
                </div>
            )}
        </DialogShell>
    )
}

function ReportTable({
    title, headers, children,
}: {
    title: string
    headers: string[]
    children: ReactNode
}) {
    return (
        <section className="overflow-hidden border">
            <div className="border-b px-4 py-3">
                <h2 className="text-sm font-semibold">{title}</h2>
            </div>
            <Table>
                <TableHeader>
                    <TableRow>
                        {headers.map((header) => <TableHead key={header}>{header}</TableHead>)}
                    </TableRow>
                </TableHeader>
                <TableBody>{children}</TableBody>
            </Table>
        </section>
    )
}

function Metric({ label, value }: { label: string; value: string }) {
    return (
        <div className="border-b p-4 sm:border-b-0 sm:border-r">
            <p className="text-xs text-muted-foreground">{label}</p>
            <p className="mt-1 text-lg font-semibold">{value}</p>
        </div>
    )
}
