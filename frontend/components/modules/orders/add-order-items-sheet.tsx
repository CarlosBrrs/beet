"use client"

import { ChevronDown, Minus, Plus, Search, ShoppingCart } from "lucide-react"
import { useMemo, useState } from "react"

import { SheetShell } from "@/components/shared/sheet-shell"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Input } from "@/components/ui/input"
import { ListPagination } from "@/components/shared/list-pagination"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { OrderDetailResponse, OrderItemRequest, PosCatalogResponse, PosTemplateOptionResponse, PosTemplateSlotResponse } from "@/lib/api-types"
import { formatCurrency } from "@/lib/formatters"
import { useDebounce } from "@/lib/hooks/use-debounce"
import { useMenus } from "@/lib/hooks/use-menus"
import { useAddOrderItems, usePosCatalog } from "@/lib/hooks/use-orders"

const ALL = "ALL"

type Selections = Record<string, Record<string, number>>

interface CartLine {
    key: string
    entry: PosCatalogResponse
    quantity: number
    selections: Selections
}

interface AddOrderItemsSheetProps {
    order: OrderDetailResponse
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function AddOrderItemsSheet({ order, open, onOpenChange }: AddOrderItemsSheetProps) {
    const [search, setSearch] = useState("")
    const [referenceType, setReferenceType] = useState<"ALL" | "PRODUCT" | "TEMPLATE">("ALL")
    const [menuId, setMenuId] = useState(ALL)
    const [submenuId, setSubmenuId] = useState(ALL)
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(12)
    const [cart, setCart] = useState<CartLine[]>([])
    const debouncedSearch = useDebounce(search, 300)
    const { data: menus = [] } = useMenus()
    const selectedMenu = menus.find((menu) => menu.id === menuId)
    const catalog = usePosCatalog({
        search: debouncedSearch,
        referenceType: referenceType === "ALL" ? undefined : referenceType,
        menuId: menuId === ALL ? undefined : menuId,
        submenuId: submenuId === ALL ? undefined : submenuId,
        page,
        size,
        sort: "menu,asc",
    })
    const addItems = useAddOrderItems(order.id)
    const total = useMemo(
        () => cart.reduce((sum, line) => sum + unitPrice(line) * line.quantity, 0),
        [cart]
    )
    const valid = cart.length > 0 && cart.every(isLineValid)

    const add = (entry: PosCatalogResponse) => {
        setCart((current) => {
            if (entry.referenceType === "PRODUCT") {
                const existing = current.find((line) => line.entry.nodeId === entry.nodeId)
                if (existing) {
                    return current.map((line) => line.key === existing.key
                        ? { ...line, quantity: limitQuantity(line, line.quantity + 1) }
                        : line)
                }
            }
            return [...current, {
                key: `${entry.nodeId}-${createLocalId()}`,
                entry,
                quantity: 1,
                selections: defaultSelections(entry),
            }]
        })
    }

    const changeQuantity = (key: string, delta: number) => {
        setCart((current) => current
            .map((line) => line.key === key
                ? { ...line, quantity: limitQuantity(line, line.quantity + delta) }
                : line)
            .filter((line) => line.quantity > 0))
    }

    const selectSingle = (key: string, slot: PosTemplateSlotResponse, option: PosTemplateOptionResponse) => {
        if (!option.available) return
        setCart((current) => current.map((line) => line.key === key ? {
            ...line,
            selections: { ...line.selections, [slot.slotId]: { [option.slotOptionId]: 1 } },
        } : line))
    }

    const changeOption = (
        key: string,
        slot: PosTemplateSlotResponse,
        option: PosTemplateOptionResponse,
        delta: number
    ) => {
        setCart((current) => current.map((line) => {
            if (line.key !== key) return line
            const slotSelections = line.selections[slot.slotId] ?? {}
            const currentQuantity = slotSelections[option.slotOptionId] ?? 0
            const selectedTotal = Object.values(slotSelections).reduce((sum, quantity) => sum + quantity, 0)
            const optionLimit = option.maxAvailableUnits === null
                ? option.maxQuantity
                : Math.min(option.maxQuantity, option.maxAvailableUnits)
            const next = Math.min(
                Math.max(currentQuantity + delta, 0),
                optionLimit,
                Math.max(0, slot.maxSelection - (selectedTotal - currentQuantity))
            )
            const nextSelections = { ...slotSelections }
            if (next === 0) delete nextSelections[option.slotOptionId]
            else nextSelections[option.slotOptionId] = next
            return { ...line, selections: { ...line.selections, [slot.slotId]: nextSelections } }
        }))
    }

    const submit = async () => {
        await addItems.mutateAsync(cart.map(toRequest))
        setCart([])
        onOpenChange(false)
    }

    return (
        <SheetShell
            open={open}
            onOpenChange={onOpenChange}
            size="full"
            title="Añadir items"
            description={order.orderStatus === "OPEN"
                ? "El lote se reservará y enviará en un ticket nuevo de cocina."
                : "El lote se reservará y aumentará el saldo pendiente; no se enviará a cocina hasta completar el pago."}
        >
            <div className="grid min-h-[calc(100vh-10rem)] gap-5 xl:grid-cols-[minmax(0,1fr)_430px]">
                <section className="space-y-4">
                    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-[minmax(240px,1fr)_180px_180px_180px]">
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                            <Input
                                value={search}
                                onChange={(event) => { setSearch(event.target.value); setPage(0) }}
                                placeholder="Buscar producto, armable, menú o submenú..."
                                className="pl-9"
                            />
                        </div>
                        <Select value={referenceType} onValueChange={(value) => {
                            setReferenceType(value as typeof referenceType)
                            setPage(0)
                        }}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                                <SelectItem value="ALL">Todos</SelectItem>
                                <SelectItem value="PRODUCT">Productos</SelectItem>
                                <SelectItem value="TEMPLATE">Armables</SelectItem>
                            </SelectContent>
                        </Select>
                        <Select value={menuId} onValueChange={(value) => {
                            setMenuId(value)
                            setSubmenuId(ALL)
                            setPage(0)
                        }}>
                            <SelectTrigger><SelectValue placeholder="Menu" /></SelectTrigger>
                            <SelectContent>
                                <SelectItem value={ALL}>Todos los menus</SelectItem>
                                {menus.map((menu) => (
                                    <SelectItem key={menu.id} value={menu.id}>{menu.name}</SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <Select
                            value={submenuId}
                            disabled={menuId === ALL}
                            onValueChange={(value) => { setSubmenuId(value); setPage(0) }}
                        >
                            <SelectTrigger><SelectValue placeholder="Submenu" /></SelectTrigger>
                            <SelectContent>
                                <SelectItem value={ALL}>Todos los submenus</SelectItem>
                                {(selectedMenu?.submenus ?? []).map((submenu) => (
                                    <SelectItem key={submenu.id} value={submenu.id}>{submenu.name}</SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                        {(catalog.data?.content ?? []).map((entry) => (
                            <div key={entry.nodeId} className="flex min-h-40 flex-col justify-between border p-4">
                                <div>
                                    <div className="flex items-start justify-between gap-2">
                                        <p className="font-medium">{entry.name}</p>
                                        <Badge variant="outline">{entry.referenceType}</Badge>
                                    </div>
                                    <p className="mt-1 text-xs text-muted-foreground">
                                        {entry.menuName} / {entry.submenuName}
                                    </p>
                                    <p className="mt-3 font-mono font-semibold">{formatCurrency(entry.price)}</p>
                                    {entry.lowStock && entry.available && (
                                        <Badge className="mt-2" variant="secondary">Stock bajo</Badge>
                                    )}
                                    {!entry.available && (
                                        <p className="mt-2 text-xs text-destructive">{entry.unavailableReason}</p>
                                    )}
                                </div>
                                <Button
                                    className="mt-4"
                                    size="sm"
                                    disabled={!entry.available}
                                    onClick={() => add(entry)}
                                >
                                    <Plus className="h-4 w-4" />
                                    Añadir
                                </Button>
                            </div>
                        ))}
                    </div>
                    <ListPagination
                        totalElements={catalog.data?.totalElements ?? 0}
                        totalPages={catalog.data?.totalPages ?? 0}
                        page={page}
                        size={size}
                        label="elemento(s)"
                        onPageChange={setPage}
                        onSizeChange={(next) => { setSize(next); setPage(0) }}
                    />
                </section>

                <aside className="border-l pl-5">
                    <div className="sticky top-0 space-y-4">
                        <div className="flex items-center justify-between">
                            <h3 className="flex items-center gap-2 font-semibold">
                                <ShoppingCart className="h-4 w-4" />
                                Nuevo lote
                            </h3>
                            <span className="font-mono font-semibold">{formatCurrency(total)}</span>
                        </div>
                        {cart.length === 0 ? (
                            <div className="border border-dashed p-8 text-center text-sm text-muted-foreground">
                                Añade uno o varios elementos para enviarlos juntos.
                            </div>
                        ) : cart.map((line) => (
                            <div key={line.key} className="border p-3">
                                <div className="flex items-start justify-between gap-3">
                                    <div>
                                        <p className="font-medium">{line.entry.name}</p>
                                        <p className="text-xs text-muted-foreground">{formatCurrency(unitPrice(line))} por unidad</p>
                                    </div>
                                    <div className="flex items-center gap-1">
                                        <Button size="icon" variant="outline" onClick={() => changeQuantity(line.key, -1)}>
                                            <Minus className="h-4 w-4" />
                                        </Button>
                                        <span className="w-8 text-center font-mono">{line.quantity}</span>
                                        <Button size="icon" variant="outline" onClick={() => changeQuantity(line.key, 1)}>
                                            <Plus className="h-4 w-4" />
                                        </Button>
                                    </div>
                                </div>
                                {line.entry.slots.map((slot) => (
                                    <Collapsible key={slot.slotId} className="mt-3 border-t pt-3">
                                        <CollapsibleTrigger className="flex w-full items-center justify-between text-left text-sm">
                                            <span>
                                                {slot.name}
                                                <span className="ml-2 text-xs text-muted-foreground">
                                                    {slotSummary(line, slot)}
                                                </span>
                                            </span>
                                            <ChevronDown className="h-4 w-4" />
                                        </CollapsibleTrigger>
                                        <CollapsibleContent className="mt-2 space-y-2">
                                            {slot.options.map((option) => {
                                                const quantity = line.selections[slot.slotId]?.[option.slotOptionId] ?? 0
                                                const single = slot.maxSelection === 1
                                                return (
                                                    <div key={option.slotOptionId} className="flex items-center justify-between gap-2 border p-2 text-sm">
                                                        <button
                                                            type="button"
                                                            disabled={!option.available}
                                                            onClick={() => single && selectSingle(line.key, slot, option)}
                                                            className="flex flex-1 items-center gap-2 text-left disabled:text-muted-foreground"
                                                        >
                                                            <span className={`h-4 w-4 border ${quantity > 0 ? "border-primary bg-primary" : ""}`} />
                                                            {option.itemName}
                                                            {option.surcharge > 0 && <span>+{formatCurrency(option.surcharge)}</span>}
                                                        </button>
                                                        {!single && (
                                                            <div className="flex items-center gap-1">
                                                                <Button size="icon" variant="ghost" onClick={() => changeOption(line.key, slot, option, -1)}>
                                                                    <Minus className="h-3 w-3" />
                                                                </Button>
                                                                <span className="w-5 text-center">{quantity}</span>
                                                                <Button size="icon" variant="ghost" disabled={!option.available} onClick={() => changeOption(line.key, slot, option, 1)}>
                                                                    <Plus className="h-3 w-3" />
                                                                </Button>
                                                            </div>
                                                        )}
                                                    </div>
                                                )
                                            })}
                                            {slotError(line, slot) && <p className="text-xs text-destructive">{slotError(line, slot)}</p>}
                                        </CollapsibleContent>
                                    </Collapsible>
                                ))}
                            </div>
                        ))}
                        <Button className="w-full" disabled={!valid || addItems.isPending} onClick={submit}>
                            {order.orderStatus === "OPEN" ? "Agregar y enviar a cocina" : "Agregar a la orden"}
                        </Button>
                    </div>
                </aside>
            </div>
        </SheetShell>
    )
}

function createLocalId() {
    return typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function defaultSelections(entry: PosCatalogResponse): Selections {
    return Object.fromEntries(entry.slots.map((slot) => {
        const selected = Object.fromEntries(slot.options
            .filter((option) => option.available && option.isDefault)
            .map((option) => [option.slotOptionId, 1]))
        if (Object.keys(selected).length === 0 && slot.minSelection === 1 && slot.maxSelection === 1) {
            const available = slot.options.filter((option) => option.available)
            if (available.length === 1) selected[available[0].slotOptionId] = 1
        }
        return [slot.slotId, selected]
    }))
}

function selectedUnits(line: CartLine, slot: PosTemplateSlotResponse) {
    return Object.values(line.selections[slot.slotId] ?? {}).reduce((sum, quantity) => sum + quantity, 0)
}

function slotError(line: CartLine, slot: PosTemplateSlotResponse) {
    const selected = selectedUnits(line, slot)
    if (selected < slot.minSelection) return `Selecciona al menos ${slot.minSelection}.`
    if (selected > slot.maxSelection) return `Selecciona máximo ${slot.maxSelection}.`
    return null
}

function slotSummary(line: CartLine, slot: PosTemplateSlotResponse) {
    const selected = slot.options
        .map((option) => ({ name: option.itemName, quantity: line.selections[slot.slotId]?.[option.slotOptionId] ?? 0 }))
        .filter((option) => option.quantity > 0)
    return selected.length === 0
        ? "Sin seleccionar"
        : selected.map((option) => option.quantity > 1 ? `${option.name} x${option.quantity}` : option.name).join(", ")
}

function isLineValid(line: CartLine) {
    return line.entry.referenceType === "PRODUCT" || line.entry.slots.every((slot) => !slotError(line, slot))
}

function limitQuantity(line: CartLine, quantity: number) {
    return line.entry.maxAvailableUnits === null ? quantity : Math.min(quantity, line.entry.maxAvailableUnits)
}

function unitPrice(line: CartLine) {
    return line.entry.price + line.entry.slots.reduce((sum, slot) =>
        sum + slot.options.reduce((optionSum, option) =>
            optionSum + option.surcharge * (line.selections[slot.slotId]?.[option.slotOptionId] ?? 0), 0), 0)
}

function toRequest(line: CartLine): OrderItemRequest {
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
                        slotOptionId: option.slotOptionId,
                        itemId: option.itemId,
                        quantity: line.selections[slot.slotId]?.[option.slotOptionId] ?? 0,
                    }))
                    .filter((option) => option.quantity > 0),
            }))
            : undefined,
    }
}
