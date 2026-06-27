"use client"

import { useMemo, useState } from "react"
import { useRouter } from "next/navigation"
import { AlertTriangle, ChevronDown, Layers3, Minus, Plus, Search, ShoppingCart } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Textarea } from "@/components/ui/textarea"
import { ListPagination } from "@/components/shared/list-pagination"
import { useDebounce } from "@/lib/hooks/use-debounce"
import { useCreateDraftOrder, useConfirmOrder, usePosCatalog } from "@/lib/hooks/use-orders"
import { useMenus } from "@/lib/hooks/use-menus"
import { useRestaurantTables } from "@/lib/hooks/use-restaurant-tables"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { formatCurrency } from "@/lib/formatters"
import { OrderItemRequest, PosCatalogResponse, PosTemplateOptionResponse, PosTemplateSlotResponse, ServiceType } from "@/lib/api-types"
import { cn } from "@/lib/utils"

type TemplateSelections = Record<string, Record<string, number>>

interface CartLine {
    key: string
    entry: PosCatalogResponse
    quantity: number
    selections?: TemplateSelections
}

function toOrderItem(line: CartLine): OrderItemRequest {
    return {
        lineType: line.entry.referenceType,
        itemId: line.entry.referenceType === "PRODUCT" ? line.entry.referenceId : undefined,
        templateId: line.entry.referenceType === "TEMPLATE" ? line.entry.referenceId : undefined,
        submenuNodeId: line.entry.nodeId,
        quantity: line.quantity,
        slots: line.entry.referenceType === "TEMPLATE"
            ? line.entry.slots.map((slot) => ({
                slotId: slot.slotId,
                options: slot.options
                    .map((option) => ({
                        option,
                        quantity: line.selections?.[slot.slotId]?.[option.slotOptionId] ?? 0,
                    }))
                    .filter(({ quantity }) => quantity > 0)
                    .map((option) => ({
                        slotOptionId: option.option.slotOptionId,
                        itemId: option.option.itemId,
                        quantity: option.quantity,
                    })),
            }))
            : undefined,
    }
}

function createCartKey(entry: PosCatalogResponse) {
    if (entry.referenceType === "PRODUCT") return entry.nodeId
    if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
        return `${entry.nodeId}-${crypto.randomUUID()}`
    }
    return `${entry.nodeId}-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function buildDefaultSelections(entry: PosCatalogResponse): TemplateSelections {
    if (entry.referenceType !== "TEMPLATE") return {}

    return entry.slots.reduce<TemplateSelections>((acc, slot) => {
        const slotSelections = slot.options.reduce<Record<string, number>>((selectionAcc, option) => {
            if (option.available && option.isDefault) {
                selectionAcc[option.slotOptionId] = 1
            }
            return selectionAcc
        }, {})

        const hasDefault = Object.values(slotSelections).some((quantity) => quantity > 0)
        const availableOptions = slot.options.filter((option) => option.available)
        if (!hasDefault && slot.minSelection === 1 && slot.maxSelection === 1 && availableOptions.length === 1) {
            slotSelections[availableOptions[0].slotOptionId] = 1
        }

        acc[slot.slotId] = slotSelections
        return acc
    }, {})
}

function getSlotSelectedUnits(line: CartLine, slot: PosTemplateSlotResponse) {
    const selections = line.selections?.[slot.slotId] ?? {}
    return slot.options.reduce((total, option) => total + (selections[option.slotOptionId] ?? 0), 0)
}

function getOptionLimit(option: PosTemplateOptionResponse) {
    return option.maxAvailableUnits === null
        ? option.maxQuantity
        : Math.min(option.maxQuantity, option.maxAvailableUnits)
}

function getSlotValidationMessage(line: CartLine, slot: PosTemplateSlotResponse) {
    const selectedUnits = getSlotSelectedUnits(line, slot)
    if (selectedUnits < slot.minSelection) {
        return `Faltan ${slot.minSelection - selectedUnits} seleccion(es).`
    }
    if (selectedUnits > slot.maxSelection) {
        return `Supera el maximo por ${selectedUnits - slot.maxSelection}.`
    }

    const selections = line.selections?.[slot.slotId] ?? {}
    const invalidOption = slot.options.find((option) => {
        const quantity = selections[option.slotOptionId] ?? 0
        return quantity > 0 && (!option.available || quantity > getOptionLimit(option))
    })
    if (invalidOption && !invalidOption.available) return `${invalidOption.itemName} no esta disponible.`
    if (invalidOption) return `${invalidOption.itemName} supera su maximo.`
    return null
}

function getSlotSummary(line: CartLine, slot: PosTemplateSlotResponse) {
    const selections = line.selections?.[slot.slotId] ?? {}
    const selected = slot.options
        .map((option) => ({
            name: option.itemName,
            quantity: selections[option.slotOptionId] ?? 0,
        }))
        .filter((option) => option.quantity > 0)

    if (selected.length === 0) return "Sin seleccionar"
    return selected
        .map((option) => option.quantity > 1 ? `${option.name} x${option.quantity}` : option.name)
        .join(", ")
}

function isTemplateLineValid(line: CartLine) {
    if (line.entry.referenceType !== "TEMPLATE") return true
    return line.entry.slots.every((slot) => !getSlotValidationMessage(line, slot))
}

function getLineUnitPrice(line: CartLine) {
    if (line.entry.referenceType !== "TEMPLATE") return line.entry.price

    const surchargeTotal = line.entry.slots.reduce((slotTotal, slot) => {
        const selections = line.selections?.[slot.slotId] ?? {}
        return slotTotal + slot.options.reduce((optionTotal, option) => {
            return optionTotal + option.surcharge * (selections[option.slotOptionId] ?? 0)
        }, 0)
    }, 0)

    return line.entry.price + surchargeTotal
}

function escapeRegExp(value: string) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
}

function HighlightMatch({ text, query }: { text: string | null | undefined; query: string }) {
    if (!text) return null
    const trimmed = query.trim()
    if (!trimmed) return <>{text}</>

    const parts = text.split(new RegExp(`(${escapeRegExp(trimmed)})`, "ig"))
    return (
        <>
            {parts.map((part, index) => (
                part.toLowerCase() === trimmed.toLowerCase()
                    ? <mark key={`${part}-${index}`} className="bg-amber-200 px-0.5 text-amber-950">{part}</mark>
                    : <span key={`${part}-${index}`}>{part}</span>
            ))}
        </>
    )
}

export default function PosPage() {
    const router = useRouter()
    const { restaurantId } = useRestaurantContext()
    const [viewMode, setViewMode] = useState<"MENU" | "ALL">("MENU")
    const [selectedMenuId, setSelectedMenuId] = useState<string>("")
    const [selectedSubmenuId, setSelectedSubmenuId] = useState<string>("")
    const [referenceType, setReferenceType] = useState<"ALL" | "PRODUCT" | "TEMPLATE">("ALL")
    const [sort, setSort] = useState("menu,asc")
    const [search, setSearch] = useState("")
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(20)
    const [serviceType, setServiceType] = useState<ServiceType>("TAKEOUT")
    const [tableId, setTableId] = useState<string>("")
    const [customerName, setCustomerName] = useState("")
    const [notes, setNotes] = useState("")
    const [cart, setCart] = useState<CartLine[]>([])
    const debouncedSearch = useDebounce(search, 300)
    const { data: menus } = useMenus()
    const effectiveMenuId = viewMode === "MENU" ? selectedMenuId || menus?.[0]?.id : undefined
    const selectedMenu = menus?.find((menu) => menu.id === effectiveMenuId)
    const effectiveSubmenuId = viewMode === "MENU" ? selectedSubmenuId || selectedMenu?.submenus[0]?.id : undefined
    const { data, isLoading } = usePosCatalog({
        search: debouncedSearch,
        page,
        size,
        menuId: effectiveMenuId,
        submenuId: effectiveSubmenuId,
        referenceType: referenceType === "ALL" ? undefined : referenceType,
        sort,
    })
    const { data: tables } = useRestaurantTables()
    const createDraft = useCreateDraftOrder()
    const confirmOrder = useConfirmOrder()
    const entries = data?.content ?? []
    const total = useMemo(
        () => cart.reduce((sum, line) => sum + getLineUnitPrice(line) * line.quantity, 0),
        [cart]
    )
    const cartIsValid = cart.every(isTemplateLineValid)
    const customerNameIsValid = customerName.trim().length > 0

    const addToCart = (entry: PosCatalogResponse) => {
        setCart((current) => {
            if (entry.referenceType === "PRODUCT") {
                const existing = current.find((line) => line.key === entry.nodeId)
                if (!existing) {
                    return [...current, { key: createCartKey(entry), entry, quantity: 1 }]
                }
                return current.map((line) =>
                    line.key === entry.nodeId
                        ? {
                            ...line,
                            quantity: entry.maxAvailableUnits === null
                                ? line.quantity + 1
                                : Math.min(line.quantity + 1, entry.maxAvailableUnits),
                        }
                        : line
                )
            }

            return [
                ...current,
                {
                    key: createCartKey(entry),
                    entry,
                    quantity: 1,
                    selections: buildDefaultSelections(entry),
                },
            ]
        })
    }

    const updateQuantity = (key: string, delta: number) => {
        setCart((current) => current
            .map((line) => {
                if (line.key !== key) return line
                const next = line.quantity + delta
                const limited = line.entry.maxAvailableUnits === null
                    ? next
                    : Math.min(next, line.entry.maxAvailableUnits)
                return { ...line, quantity: limited }
            })
            .filter((line) => line.quantity > 0))
    }

    const updateOptionQuantity = (
        lineKey: string,
        slot: PosTemplateSlotResponse,
        option: PosTemplateOptionResponse,
        delta: number
    ) => {
        setCart((current) => current.map((line) => {
            if (line.key !== lineKey || line.entry.referenceType !== "TEMPLATE") return line

            const currentSlotSelections = line.selections?.[slot.slotId] ?? {}
            const currentQuantity = currentSlotSelections[option.slotOptionId] ?? 0
            const currentSlotTotal = getSlotSelectedUnits(line, slot)
            const maxAllowedBySlot = Math.max(0, slot.maxSelection - (currentSlotTotal - currentQuantity))
            const nextQuantity = Math.min(
                Math.max(currentQuantity + delta, 0),
                option.maxQuantity,
                maxAllowedBySlot
            )
            const nextSlotSelections = { ...currentSlotSelections }
            if (nextQuantity > 0) {
                nextSlotSelections[option.slotOptionId] = nextQuantity
            } else {
                delete nextSlotSelections[option.slotOptionId]
            }

            return {
                ...line,
                selections: {
                    ...(line.selections ?? {}),
                    [slot.slotId]: nextSlotSelections,
                },
            }
        }))
    }

    const replaceSingleSlotOption = (
        lineKey: string,
        slot: PosTemplateSlotResponse,
        option: PosTemplateOptionResponse
    ) => {
        if (!option.available) return

        setCart((current) => current.map((line) => {
            if (line.key !== lineKey || line.entry.referenceType !== "TEMPLATE") return line

            return {
                ...line,
                selections: {
                    ...(line.selections ?? {}),
                    [slot.slotId]: {
                        [option.slotOptionId]: 1,
                    },
                },
            }
        }))
    }

    const setOptionQuantity = (
        lineKey: string,
        slot: PosTemplateSlotResponse,
        option: PosTemplateOptionResponse,
        quantity: number
    ) => {
        setCart((current) => current.map((line) => {
            if (line.key !== lineKey || line.entry.referenceType !== "TEMPLATE") return line

            const currentSlotSelections = line.selections?.[slot.slotId] ?? {}
            const currentQuantity = currentSlotSelections[option.slotOptionId] ?? 0
            const currentSlotTotal = getSlotSelectedUnits(line, slot)
            const maxAllowedBySlot = Math.max(0, slot.maxSelection - (currentSlotTotal - currentQuantity))
            const nextQuantity = Math.min(Math.max(quantity, 0), option.maxQuantity, maxAllowedBySlot)
            const nextSlotSelections = { ...currentSlotSelections }
            if (nextQuantity > 0) {
                nextSlotSelections[option.slotOptionId] = nextQuantity
            } else {
                delete nextSlotSelections[option.slotOptionId]
            }

            return {
                ...line,
                selections: {
                    ...(line.selections ?? {}),
                    [slot.slotId]: nextSlotSelections,
                },
            }
        }))
    }

    const submit = async () => {
        const draft = await createDraft.mutateAsync({
            serviceType,
            tableId: serviceType === "DINE_IN" ? tableId : undefined,
            customerName: customerName.trim(),
            notes: notes || undefined,
            items: cart.map(toOrderItem),
        })
        const confirmed = await confirmOrder.mutateAsync(draft.id)
        setCart([])
        setCustomerName("")
        setNotes("")
        if (confirmed.orderStatus === "AWAITING_PAYMENT") {
            router.push(`/restaurants/${restaurantId}/orders/${confirmed.id}/payments`)
            return
        }
        router.push(`/restaurants/${restaurantId}/orders`)
    }

    const canSubmit = cart.length > 0
        && cartIsValid
        && customerNameIsValid
        && (serviceType !== "DINE_IN" || !!tableId)
        && !createDraft.isPending
        && !confirmOrder.isPending

    return (
        <div className="grid min-h-[calc(100vh-8rem)] gap-6 pb-10 xl:grid-cols-[minmax(0,1fr)_460px] 2xl:grid-cols-[minmax(0,1fr)_520px]">
            <div className="space-y-5">
                <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
                    <div>
                        <h1 className="text-3xl font-bold tracking-tight">POS</h1>
                        <p className="mt-1 text-muted-foreground">Navega por menu/submenu o busca en todo el catalogo vendible.</p>
                    </div>
                    <div className="relative w-full md:w-80">
                        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                        <Input
                            value={search}
                            onChange={(event) => { setSearch(event.target.value); setPage(0) }}
                            placeholder="Buscar producto o armable..."
                            className="pl-9"
                        />
                    </div>
                </div>

                <div className="space-y-3 border bg-muted/20 p-3">
                    <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                        <div className="flex flex-wrap gap-2">
                            <Button
                                type="button"
                                variant={viewMode === "MENU" ? "default" : "outline"}
                                size="sm"
                                onClick={() => { setViewMode("MENU"); setPage(0) }}
                            >
                                <Layers3 className="mr-2 h-4 w-4" />
                                Menu
                            </Button>
                            <Button
                                type="button"
                                variant={viewMode === "ALL" ? "default" : "outline"}
                                size="sm"
                                onClick={() => { setViewMode("ALL"); setPage(0) }}
                            >
                                Todo el catalogo
                            </Button>
                            <Button
                                type="button"
                                variant={referenceType === "ALL" ? "default" : "outline"}
                                size="sm"
                                onClick={() => { setReferenceType("ALL"); setPage(0) }}
                            >
                                Todos
                            </Button>
                            <Button
                                type="button"
                                variant={referenceType === "PRODUCT" ? "default" : "outline"}
                                size="sm"
                                onClick={() => { setReferenceType("PRODUCT"); setPage(0) }}
                            >
                                Productos
                            </Button>
                            <Button
                                type="button"
                                variant={referenceType === "TEMPLATE" ? "default" : "outline"}
                                size="sm"
                                onClick={() => { setReferenceType("TEMPLATE"); setPage(0) }}
                            >
                                Armables
                            </Button>
                        </div>
                        <Select value={sort} onValueChange={(value) => { setSort(value); setPage(0) }}>
                            <SelectTrigger className="w-full lg:w-52"><SelectValue /></SelectTrigger>
                            <SelectContent>
                                <SelectItem value="menu,asc">Menu / submenu</SelectItem>
                                <SelectItem value="name,asc">Nombre A-Z</SelectItem>
                                <SelectItem value="name,desc">Nombre Z-A</SelectItem>
                                <SelectItem value="price,asc">Menor precio</SelectItem>
                                <SelectItem value="price,desc">Mayor precio</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>

                    {viewMode === "MENU" && (
                        <div className="space-y-3">
                            <Tabs
                                value={effectiveMenuId ?? ""}
                                onValueChange={(value) => {
                                    setSelectedMenuId(value)
                                    setSelectedSubmenuId("")
                                    setPage(0)
                                }}
                            >
                                <TabsList className="h-auto flex-wrap justify-start">
                                    {(menus ?? []).map((menu) => (
                                        <TabsTrigger key={menu.id} value={menu.id}>
                                            {menu.name}
                                        </TabsTrigger>
                                    ))}
                                </TabsList>
                            </Tabs>
                            <div className="flex flex-wrap gap-2">
                                {(selectedMenu?.submenus ?? []).map((submenu) => {
                                    const active = effectiveSubmenuId === submenu.id
                                    return (
                                        <Button
                                            key={submenu.id}
                                            type="button"
                                            variant={active ? "default" : "outline"}
                                            size="sm"
                                            onClick={() => { setSelectedSubmenuId(submenu.id); setPage(0) }}
                                        >
                                            {submenu.name}
                                        </Button>
                                    )
                                })}
                            </div>
                        </div>
                    )}
                </div>

                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4">
                    {isLoading ? (
                        Array.from({ length: 8 }).map((_, index) => (
                            <div key={index} className="h-44 animate-pulse border bg-muted/40" />
                        ))
                    ) : entries.length === 0 ? (
                        <div className="col-span-full border border-dashed bg-muted/20 p-12 text-center text-sm text-muted-foreground">
                            No hay productos publicados para vender.
                        </div>
                    ) : entries.map((entry) => (
                        <button
                            key={entry.nodeId}
                            type="button"
                            disabled={!entry.available}
                            onClick={() => addToCart(entry)}
                            className="min-h-44 border bg-background p-4 text-left transition hover:bg-muted/40 disabled:cursor-not-allowed disabled:bg-muted/40 disabled:text-muted-foreground"
                            title={!entry.available ? entry.unavailableReason ?? "No disponible" : undefined}
                        >
                            <div className="flex items-start justify-between gap-3">
                                <div>
                                    <p className="font-semibold">
                                        <HighlightMatch text={entry.name} query={debouncedSearch} />
                                    </p>
                                    <p className="mt-1 text-xs text-muted-foreground">
                                        <HighlightMatch text={entry.menuName} query={debouncedSearch} />
                                        {" / "}
                                        <HighlightMatch text={entry.submenuName} query={debouncedSearch} />
                                    </p>
                                </div>
                                <Badge variant={entry.referenceType === "TEMPLATE" ? "secondary" : "outline"}>
                                    {entry.referenceType === "TEMPLATE" ? "Armable" : "Producto"}
                                </Badge>
                            </div>
                            {entry.description && (
                                <p className="mt-3 line-clamp-2 text-xs text-muted-foreground">
                                    <HighlightMatch text={entry.description} query={debouncedSearch} />
                                </p>
                            )}
                            <p className="mt-4 font-mono text-lg">{formatCurrency(entry.price)}</p>
                            <div className="mt-3 flex flex-wrap gap-2">
                                {entry.lowStock && <Badge variant="outline" className="border-amber-500 text-amber-700">Stock bajo</Badge>}
                                {entry.maxAvailableUnits !== null && entry.available && (
                                    <Badge variant="outline">Máx. {entry.maxAvailableUnits}</Badge>
                                )}
                                {!entry.available && <Badge variant="destructive">Agotado</Badge>}
                            </div>
                            {entry.unavailableReason && (
                                <p className="mt-3 flex gap-2 text-xs text-muted-foreground">
                                    <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                                    {entry.unavailableReason}
                                </p>
                            )}
                        </button>
                    ))}
                </div>

                <ListPagination
                    totalElements={data?.totalElements ?? 0}
                    totalPages={data?.totalPages ?? 0}
                    page={page}
                    size={size}
                    label="item(s)"
                    onPageChange={setPage}
                    onSizeChange={(nextSize) => { setSize(nextSize); setPage(0) }}
                />
            </div>

            <Card className="h-fit">
                <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-lg">
                        <ShoppingCart className="h-5 w-5" />
                        Orden
                    </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="grid gap-3">
                        <Label>Servicio</Label>
                        <Select value={serviceType} onValueChange={(value) => setServiceType(value as ServiceType)}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                                <SelectItem value="TAKEOUT">Para llevar</SelectItem>
                                <SelectItem value="DINE_IN">Mesa</SelectItem>
                                <SelectItem value="DELIVERY">Domicilio</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>

                    {serviceType === "DINE_IN" && (
                        <div className="grid gap-3">
                            <Label>Mesa</Label>
                            <Select value={tableId} onValueChange={setTableId}>
                                <SelectTrigger><SelectValue placeholder="Selecciona mesa" /></SelectTrigger>
                                <SelectContent>
                                    {(tables ?? []).filter((table) => table.isActive).map((table) => (
                                        <SelectItem key={table.id} value={table.id}>
                                            {table.name} · {table.availabilityStatus}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    )}

                    <div className="grid gap-3">
                        <Label>Cliente</Label>
                        <Input value={customerName} onChange={(event) => setCustomerName(event.target.value)} />
                        {!customerNameIsValid && (
                            <p className="text-xs text-destructive">El nombre del cliente es obligatorio.</p>
                        )}
                    </div>

                    <div className="space-y-3">
                        {cart.length === 0 ? (
                            <div className="border border-dashed bg-muted/20 p-8 text-center text-sm text-muted-foreground">
                                Agrega productos desde el catalogo.
                            </div>
                        ) : cart.map((line) => {
                            const lineUnitPrice = getLineUnitPrice(line)
                            const lineIsValid = isTemplateLineValid(line)

                            return (
                                <div
                                    key={line.key}
                                    className={cn(
                                        "space-y-3 border p-3",
                                        !lineIsValid && "border-destructive/50 bg-destructive/5"
                                    )}
                                >
                                    <div className="flex items-center justify-between gap-3">
                                        <div className="min-w-0">
                                            <div className="flex items-center gap-2">
                                                <p className="truncate font-medium">{line.entry.name}</p>
                                                {line.entry.referenceType === "TEMPLATE" && (
                                                    <Badge variant="secondary">Armable</Badge>
                                                )}
                                            </div>
                                            <p className="text-xs text-muted-foreground">
                                                {formatCurrency(lineUnitPrice)}
                                                {line.entry.referenceType === "TEMPLATE" && lineUnitPrice !== line.entry.price && (
                                                    <> · base {formatCurrency(line.entry.price)}</>
                                                )}
                                            </p>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <Button variant="outline" size="icon" onClick={() => updateQuantity(line.key, -1)}>
                                                <Minus className="h-4 w-4" />
                                            </Button>
                                            <span className="w-8 text-center text-sm">{line.quantity}</span>
                                            <Button
                                                variant="outline"
                                                size="icon"
                                                disabled={line.entry.maxAvailableUnits !== null
                                                    && line.quantity >= line.entry.maxAvailableUnits}
                                                onClick={() => updateQuantity(line.key, 1)}
                                            >
                                                <Plus className="h-4 w-4" />
                                            </Button>
                                        </div>
                                    </div>

                                    {line.entry.referenceType === "TEMPLATE" && (
                                        <div className="space-y-3 border-t pt-3">
                                            {line.entry.slots.map((slot) => {
                                                const selectedUnits = getSlotSelectedUnits(line, slot)
                                                const validationMessage = getSlotValidationMessage(line, slot)
                                                const isFixedSingleOption = slot.options.length === 1
                                                    && slot.minSelection === 1
                                                    && slot.maxSelection === 1
                                                const isSingleSelection = slot.maxSelection === 1
                                                const selectedSummary = getSlotSummary(line, slot)
                                                const selectedOptionId = isSingleSelection
                                                    ? slot.options.find((option) => (line.selections?.[slot.slotId]?.[option.slotOptionId] ?? 0) > 0)?.slotOptionId
                                                    : undefined

                                                return (
                                                    <Collapsible
                                                        key={slot.slotId}
                                                        defaultOpen={!!validationMessage}
                                                        className="bg-muted/20"
                                                    >
                                                        <CollapsibleTrigger asChild>
                                                            <button
                                                                type="button"
                                                                className="group flex w-full items-start justify-between gap-3 p-2 text-left"
                                                            >
                                                            <div className="min-w-0">
                                                                <p className="text-sm font-medium">{slot.name}</p>
                                                                <p className="mt-1 truncate text-xs text-muted-foreground">{selectedSummary}</p>
                                                                <p className="text-xs text-muted-foreground">
                                                                    {selectedUnits}/{slot.maxSelection} seleccionado · minimo {slot.minSelection}
                                                                </p>
                                                            </div>
                                                            {validationMessage ? (
                                                                <Badge variant="destructive">{validationMessage}</Badge>
                                                            ) : (
                                                                <Badge variant="outline">Valido</Badge>
                                                            )}
                                                            <ChevronDown className="mt-1 h-4 w-4 shrink-0 text-muted-foreground transition-transform group-data-[state=open]:rotate-180" />
                                                            </button>
                                                        </CollapsibleTrigger>

                                                        <CollapsibleContent className="space-y-2 border-t p-2">
                                                            {false && (
                                                            <Select
                                                                key={`${line.key}-${slot.slotId}`}
                                                                value={selectedOptionId}
                                                                onValueChange={(optionId) => {
                                                                    const option = slot.options.find((candidate) => candidate.slotOptionId === optionId)
                                                                    if (!option) return
                                                                    if (slot.maxSelection === 1) {
                                                                        replaceSingleSlotOption(line.key, slot, option)
                                                                        return
                                                                    }
                                                                    updateOptionQuantity(line.key, slot, option, 1)
                                                                }}
                                                                disabled={isFixedSingleOption || (selectedUnits >= slot.maxSelection && slot.maxSelection !== 1)}
                                                            >
                                                                <SelectTrigger className="h-9 bg-background">
                                                                    <SelectValue
                                                                        placeholder={
                                                                            selectedUnits >= slot.maxSelection && slot.maxSelection !== 1
                                                                                ? "Maximo alcanzado"
                                                                                : slot.maxSelection === 1
                                                                                    ? "Seleccionar opcion"
                                                                                    : "Agregar opcion"
                                                                        }
                                                                    />
                                                                </SelectTrigger>
                                                                <SelectContent>
                                                                    {slot.options.map((option) => {
                                                                        const optionQuantity = line.selections?.[slot.slotId]?.[option.slotOptionId] ?? 0
                                                                        const disabled = !option.available
                                                                            || (slot.maxSelection !== 1 && optionQuantity >= getOptionLimit(option))
                                                                        return (
                                                                            <SelectItem
                                                                                key={option.slotOptionId}
                                                                                value={option.slotOptionId}
                                                                                disabled={disabled}
                                                                            >
                                                                                {option.itemName}
                                                                                {option.surcharge > 0 ? ` +${formatCurrency(option.surcharge)}` : ""}
                                                                                {!option.available ? " · agotado" : ""}
                                                                                {optionQuantity >= option.maxQuantity ? " · maximo" : ""}
                                                                            </SelectItem>
                                                                        )
                                                                    })}
                                                                </SelectContent>
                                                            </Select>
                                                            )}

                                                            {slot.options.map((option) => {
                                                                const optionQuantity = line.selections?.[slot.slotId]?.[option.slotOptionId] ?? 0
                                                                const checked = optionQuantity > 0
                                                                const canSelect = option.available
                                                                    && (checked || selectedUnits < slot.maxSelection)
                                                                const canIncrease = option.available
                                                                    && optionQuantity < getOptionLimit(option)
                                                                    && selectedUnits < slot.maxSelection

                                                                return (
                                                                    <div
                                                                        key={option.slotOptionId}
                                                                        className={cn(
                                                                            "flex items-center justify-between gap-2 border bg-background p-2",
                                                                            optionQuantity > 0 && "border-primary/50 bg-primary/5",
                                                                            !option.available && "bg-muted text-muted-foreground"
                                                                        )}
                                                                    >
                                                                        <div className="flex min-w-0 items-start gap-2">
                                                                            {isSingleSelection ? (
                                                                                <input
                                                                                    type="radio"
                                                                                    name={`${line.key}-${slot.slotId}`}
                                                                                    checked={checked}
                                                                                    disabled={!option.available || isFixedSingleOption}
                                                                                    onChange={() => replaceSingleSlotOption(line.key, slot, option)}
                                                                                    className="mt-1 size-4 shrink-0 accent-primary"
                                                                                />
                                                                            ) : (
                                                                                <Checkbox
                                                                                    checked={checked}
                                                                                    disabled={!canSelect}
                                                                                    onCheckedChange={(nextChecked) => {
                                                                                        setOptionQuantity(line.key, slot, option, nextChecked ? 1 : 0)
                                                                                    }}
                                                                                    className="mt-1"
                                                                                />
                                                                            )}
                                                                            <div className="min-w-0">
                                                                            <div className="flex flex-wrap items-center gap-2">
                                                                                <p className="truncate text-sm font-medium">{option.itemName}</p>
                                                                                {option.surcharge > 0 && (
                                                                                    <Badge variant="outline">+{formatCurrency(option.surcharge)}</Badge>
                                                                                )}
                                                                                {option.lowStock && (
                                                                                    <Badge variant="outline" className="border-amber-500 text-amber-700">Stock bajo</Badge>
                                                                                )}
                                                                                {!option.available && <Badge variant="destructive">Agotado</Badge>}
                                                                            </div>
                                                                            <p className="text-xs text-muted-foreground">
                                                                                Max {getOptionLimit(option)}
                                                                                {option.maxAvailableUnits !== null && " por inventario"}
                                                                                {option.isDefault && " · predeterminado"}
                                                                                {isFixedSingleOption && " · fijo"}
                                                                            </p>
                                                                            {option.unavailableReason && (
                                                                                <p className="mt-1 text-xs text-muted-foreground">{option.unavailableReason}</p>
                                                                            )}
                                                                        </div>
                                                                        {!isSingleSelection && checked && (
                                                                        <div className="flex shrink-0 items-center gap-2">
                                                                            <Button
                                                                                type="button"
                                                                                variant="outline"
                                                                                size="icon"
                                                                                disabled={optionQuantity <= 0}
                                                                                onClick={() => updateOptionQuantity(line.key, slot, option, -1)}
                                                                            >
                                                                                <Minus className="h-4 w-4" />
                                                                            </Button>
                                                                            <span className="w-8 text-center text-sm">{optionQuantity}</span>
                                                                            <Button
                                                                                type="button"
                                                                                variant="outline"
                                                                                size="icon"
                                                                                disabled={!canIncrease}
                                                                                onClick={() => updateOptionQuantity(line.key, slot, option, 1)}
                                                                            >
                                                                                <Plus className="h-4 w-4" />
                                                                            </Button>
                                                                        </div>
                                                                        )}
                                                                    </div>
                                                                    </div>
                                                                )
                                                            })}

                                                            {selectedUnits === 0 && (
                                                                <p className="border border-dashed bg-background p-2 text-xs text-muted-foreground">
                                                                    No hay opciones seleccionadas en este slot.
                                                                </p>
                                                            )}
                                                        </CollapsibleContent>
                                                    </Collapsible>
                                                )
                                            })}
                                        </div>
                                    )}
                                </div>
                            )
                        })}
                    </div>

                    <Textarea value={notes} onChange={(event) => setNotes(event.target.value)} placeholder="Notas de la orden" />
                    <div className="flex items-center justify-between border-t pt-4">
                        <span className="font-medium">Total bruto</span>
                        <span className="font-mono text-xl font-semibold">{formatCurrency(total)}</span>
                    </div>
                    <Button className="w-full" disabled={!canSubmit} onClick={submit}>
                        Confirmar orden
                    </Button>
                    <p className="text-xs text-muted-foreground">
                        En prepago la orden queda esperando pago; en pospago entra abierta y genera ticket de cocina.
                    </p>
                </CardContent>
            </Card>
        </div>
    )
}
