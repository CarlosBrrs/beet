"use client"

import * as React from "react"
import { useForm, useFieldArray, useWatch } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Button } from "@/components/ui/button"
import {
    Form,
    FormControl,
    FormDescription,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Switch } from "@/components/ui/switch"
import { Card, CardContent } from "@/components/ui/card"
import { Check, ChevronsUpDown, Info, Loader2, Plus, Trash2 } from "lucide-react"
import { formatPriceDisplay, parsePriceInput } from "@/lib/formatters"
import { useUnits } from "@/lib/hooks/use-units"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command"
import { cn } from "@/lib/utils"
import { useInventoryStocks } from "@/lib/hooks/use-inventory"
import { useCreateCatalogProduct, usePreparations, useProductById, useUpdateProduct } from "@/lib/hooks/use-items"
import { useCreateSubmenuProduct, useUpdateSubmenuProduct, useProduct } from "@/lib/hooks/use-submenu-nodes"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { ItemResponse } from "@/lib/api-types"

// ── Inline Combobox ──────────────────────────────────────────────────────────

function ItemCombobox({
    items,
    value,
    onChange,
    placeholder = "Buscar...",
}: {
    items: { id: string; name: string }[]
    value?: string
    onChange: (id: string) => void
    placeholder?: string
}) {
    const [open, setOpen] = React.useState(false)
    const selected = items.find((i) => i.id === value)

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <Button
                    variant="outline"
                    role="combobox"
                    aria-expanded={open}
                    className="w-full justify-between h-8 text-xs font-normal px-2"
                >
                    <span className="truncate">{selected ? selected.name : placeholder}</span>
                    <ChevronsUpDown className="ml-2 h-3 w-3 shrink-0 opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-[280px] p-0" align="start">
                <Command filter={(value, search) => {
                    if (value.toLowerCase().includes(search.toLowerCase())) return 1;
                    return 0;
                }}>
                    <CommandInput placeholder={placeholder} className="h-9 text-xs" />
                    <CommandList>
                        <CommandEmpty>No se encontraron resultados.</CommandEmpty>
                        <CommandGroup>
                            {items.map((item) => (
                                <CommandItem
                                    key={item.id}
                                    value={item.name}
                                    onSelect={() => { onChange(item.id); setOpen(false) }}
                                    className="text-xs"
                                >
                                    <Check className={cn("mr-2 h-4 w-4", value === item.id ? "opacity-100" : "opacity-0")} />
                                    {item.name}
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    )
}

// ── Zod Schema ───────────────────────────────────────────────────────────────

const recipeLineSchema = z.object({
    source: z.enum(["INGREDIENT", "PREPARATION"]),
    masterIngredientId: z.string().optional(),
    childItemId: z.string().optional(),
    quantity: z.string().min(1, "Requerido"),
    unitId: z.string().min(1, "Selecciona una unidad"),
}).superRefine((data, ctx) => {
    if (data.source === "INGREDIENT" && !data.masterIngredientId) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Selecciona un insumo", path: ["masterIngredientId"] })
    }
    if (data.source === "PREPARATION" && !data.childItemId) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Selecciona una preparación", path: ["childItemId"] })
    }
})

const productSchema = z.object({
    name: z.string().min(2, "Mínimo 2 caracteres"),
    description: z.string().optional(),
    salePrice: z.string().optional(),
    isInventoryTracked: z.boolean(),
    isAvailableAsTemplateOption: z.boolean(),
    userDefinedCost: z.string().optional(),
    lines: z.array(recipeLineSchema).optional(),
    yieldQty: z.number().positive().optional(),
    yieldUnitId: z.string().optional(),
    sellableUnitsPerBatch: z.number().int().positive().optional(),
}).superRefine((data, ctx) => {
    if (data.isInventoryTracked && (!data.lines || data.lines.length === 0)) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "La receta debe tener al menos un elemento", path: ["lines"] })
    }
    if (data.isInventoryTracked && !data.sellableUnitsPerBatch) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Define cuántas porciones produce el lote", path: ["sellableUnitsPerBatch"] })
    }
})

type ProductFormValues = z.infer<typeof productSchema>

// ── Component ────────────────────────────────────────────────────────────────

interface ProductFormProps {
    menuId?: string
    submenuId?: string
    productId?: string | null
    forceAvailableAsTemplateOption?: boolean
    onSuccess?: (product: ItemResponse) => void
}

export function ProductForm({ menuId, submenuId, productId, forceAvailableAsTemplateOption = false, onSuccess }: ProductFormProps) {
    const { restaurantId } = useRestaurantContext()
    const { data: units = [] } = useUnits()
    const isCatalogMode = !menuId || !submenuId
    const createSubmenuProduct = useCreateSubmenuProduct(menuId ?? "", submenuId ?? "")
    const updateSubmenuProduct = useUpdateSubmenuProduct(menuId ?? "", submenuId ?? "", productId || "")
    const createCatalogProduct = useCreateCatalogProduct()
    const updateCatalogProduct = useUpdateProduct()
    const submenuProduct = useProduct(menuId, submenuId, isCatalogMode ? undefined : productId || undefined)
    const catalogProduct = useProductById(isCatalogMode ? productId : undefined)
    const initialProduct = isCatalogMode ? catalogProduct.data : submenuProduct.data
    const isLoadingInitial = isCatalogMode ? catalogProduct.isLoading : submenuProduct.isLoading

    // Fetch only ingredients activated in this restaurant's inventory
    const { data: inventoryData } = useInventoryStocks({
        restaurantId: restaurantId!,
        page: 0,
        size: 200
    })
    const ingredients = inventoryData?.content ?? []
    const ingredientOptions = ingredients.map(i => ({
        id: i.masterIngredientId,
        name: i.ingredientName
    }))

    const { data: preparations = [] } = usePreparations()

    const form = useForm<ProductFormValues>({
        resolver: zodResolver(productSchema),
        defaultValues: {
            name: "",
            description: "",
            salePrice: "",
            isInventoryTracked: false,
            isAvailableAsTemplateOption: forceAvailableAsTemplateOption,
            userDefinedCost: "",
            lines: [],
            sellableUnitsPerBatch: 1,
        },
    })

    React.useEffect(() => {
        if (initialProduct) {
            form.reset({
                name: initialProduct.name,
                description: initialProduct.description || "",
                salePrice: initialProduct.salePrice?.toString() || "",
                isInventoryTracked: initialProduct.isInventoryTracked,
                isAvailableAsTemplateOption: initialProduct.isAvailableAsTemplateOption,
                userDefinedCost: initialProduct.theoreticalCost?.toString() || "",
                yieldQty: initialProduct.yieldQty || undefined,
                yieldUnitId: initialProduct.yieldUnitId || undefined,
                sellableUnitsPerBatch: initialProduct.sellableUnitsPerBatch || 1,
                lines: initialProduct.recipeLines?.map(line => ({
                    source: line.source,
                    masterIngredientId: line.masterIngredientId || undefined,
                    childItemId: line.childItemId || undefined,
                    quantity: line.quantity.toString(),
                    unitId: line.unitId,
                })) || [],
            });
        }
    }, [initialProduct, form])

    const { fields, append, remove } = useFieldArray({ control: form.control, name: "lines" })

    const watchIsTracked = useWatch({ control: form.control, name: "isInventoryTracked" })
    const watchLines = useWatch({ control: form.control, name: "lines" })
    const watchYieldQty = useWatch({ control: form.control, name: "yieldQty" })
    const watchYieldUnitId = useWatch({ control: form.control, name: "yieldUnitId" })
    const watchSellableUnits = useWatch({ control: form.control, name: "sellableUnitsPerBatch" })
    const selectedYieldUnit = units.find(unit => unit.id === watchYieldUnitId)
    const derivedPortionSize = watchYieldQty && watchSellableUnits
        ? watchYieldQty / watchSellableUnits
        : null
    const isPending = createSubmenuProduct.isPending || updateSubmenuProduct.isPending
        || createCatalogProduct.isPending || updateCatalogProduct.isPending

    if (productId && isLoadingInitial) {
        return (
            <div className="flex justify-center p-8">
                <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
        )
    }

    /**
     * Returns the base unit type (WEIGHT / VOLUME / UNIT) for the selected
     * ingredient or preparation on a given line, so we can filter the unit dropdown.
     */
    const getUnitTypeForLine = (index: number) => {
        const line = watchLines?.[index]
        if (!line) return null

        if (line.source === "INGREDIENT" && line.masterIngredientId) {
            const ingredient = ingredients.find(i => i.masterIngredientId === line.masterIngredientId)
            if (ingredient?.unitAbbreviation) {
                const unit = units.find(u => u.abbreviation === ingredient.unitAbbreviation)
                return unit?.type ?? null
            }
        } else if (line.source === "PREPARATION" && line.childItemId) {
            const prep = preparations.find(p => p.id === line.childItemId)
            if (prep?.yieldUnitId) {
                const unit = units.find(u => u.id === prep.yieldUnitId)
                return unit?.type ?? null
            }
        }
        return null
    }

    const onSubmit = (values: ProductFormValues) => {
        const payload: import("@/lib/api-types").CreateProductRequest = {
            name: values.name,
            description: values.description,
            isInventoryTracked: values.isInventoryTracked,
            isAvailableAsTemplateOption: forceAvailableAsTemplateOption || values.isAvailableAsTemplateOption,
        }
        if (values.salePrice) payload.salePrice = parsePriceInput(values.salePrice)

        if (values.isInventoryTracked && values.lines) {
            payload.lines = values.lines.map(line => ({
                source: line.source,
                masterIngredientId: line.source === "INGREDIENT" ? line.masterIngredientId : undefined,
                childItemId: line.source === "PREPARATION" ? line.childItemId : undefined,
                quantity: parsePriceInput(line.quantity),
                unitId: line.unitId,
            }))
            if (values.yieldQty && values.yieldUnitId) {
                payload.yieldQty = values.yieldQty
                payload.yieldUnitId = values.yieldUnitId
            }
            payload.sellableUnitsPerBatch = values.sellableUnitsPerBatch
        } else {
            // Flat products require a default yield unit in the database
            const pcsUnit = units.find(u => u.abbreviation === "pcs")
            payload.yieldQty = 1
            payload.yieldUnitId = pcsUnit?.id
            payload.sellableUnitsPerBatch = 1
            payload.userDefinedCost = values.userDefinedCost ? parsePriceInput(values.userDefinedCost) : 0
        }

        if (productId) {
            if (isCatalogMode) {
                updateCatalogProduct.mutate({ id: productId, data: payload }, { onSuccess: (product) => onSuccess?.(product) })
            } else {
                updateSubmenuProduct.mutate(payload, { onSuccess: (product) => onSuccess?.(product) })
            }
        } else {
            if (isCatalogMode) {
                createCatalogProduct.mutate(payload, { onSuccess: (product) => onSuccess?.(product) })
            } else {
                createSubmenuProduct.mutate(payload, { onSuccess: (product) => onSuccess?.(product) })
            }
        }
    }

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">

                <div className="space-y-4">
                    <FormField
                        control={form.control}
                        name="name"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Nombre del Producto</FormLabel>
                                <FormControl>
                                    <Input placeholder="Ej: Hamburguesa Clásica" {...field} />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="description"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Descripción (opcional)</FormLabel>
                                <FormControl>
                                    <Textarea placeholder="Descripción para el cliente..." className="resize-none" {...field} value={field.value ?? ""} />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="salePrice"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Precio de Venta {isCatalogMode && "(opcional)"}</FormLabel>
                                <FormDescription>
                                    Necesario para publicar el producto individualmente en un submenu. Puede quedar vacio si se usara solo como opcion de un armable.
                                </FormDescription>
                                <FormControl>
                                    <div className="relative">
                                        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">$</span>
                                        <Input
                                            type="text"
                                            inputMode="decimal"
                                            className="pl-7"
                                            {...field}
                                            onBlur={e => field.onChange(formatPriceDisplay(e.target.value))}
                                        />
                                    </div>
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="isAvailableAsTemplateOption"
                        render={({ field }) => (
                            <FormItem className="flex items-center justify-between gap-4 rounded border p-3">
                                <div>
                                    <FormLabel>Disponible como opción para armables</FormLabel>
                                    <FormDescription>
                                        Permite seleccionar este producto dentro de los slots de un armable. No lo publica individualmente en el menu.
                                    </FormDescription>
                                </div>
                                <FormControl>
                                    <Switch checked={forceAvailableAsTemplateOption || field.value} disabled={forceAvailableAsTemplateOption} onCheckedChange={field.onChange} />
                                </FormControl>
                            </FormItem>
                        )}
                    />

                    <div className="flex gap-6 pt-2">
                        <FormField
                            control={form.control}
                            name="isInventoryTracked"
                            render={({ field }) => (
                                <FormItem className="flex flex-row items-center justify-between rounded-lg border p-3 shadow-sm w-full">
                                    <div className="space-y-0.5">
                                        <FormLabel>Rastrear en Inventario</FormLabel>
                                        <FormDescription>
                                            {productId
                                                ? "Se define al crear el producto. Cambiarlo despues requiere migrar su modelo de costo y receta."
                                                : "Activa esta opcion si el costo se calculara desde una receta y sus existencias deben rastrearse."}
                                        </FormDescription>
                                    </div>
                                    <FormControl>
                                        <Switch checked={field.value} onCheckedChange={field.onChange} disabled={!!productId} />
                                    </FormControl>
                                </FormItem>
                            )}
                        />

                    </div>
                    {productId && initialProduct && !initialProduct.isInventoryTracked && (
                        <div className="flex gap-3 border border-amber-300 bg-amber-50 p-3 text-sm text-amber-950 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-100">
                            <Info className="mt-0.5 h-4 w-4 shrink-0" />
                            <div>
                                <p className="font-medium">Pendiente: convertir productos planos en rastreados</p>
                                <p className="mt-1 text-xs leading-relaxed">
                                    Este producto conserva por ahora su costo manual. A futuro se agregara un flujo dedicado
                                    para incorporar receta, rendimiento y costo calculado sin perder sus publicaciones,
                                    usos dentro de armables ni referencias historicas.
                                </p>
                            </div>
                        </div>
                    )}
                </div>

                {!watchIsTracked ? (
                    <FormField
                        control={form.control}
                        name="userDefinedCost"
                        render={({ field }) => (
                            <FormItem className="bg-muted/30 p-4 rounded-lg border">
                                <FormLabel>Costo Teórico Manual (opcional)</FormLabel>
                                <FormDescription>Como no tiene receta, ingresa un estimado de costo.</FormDescription>
                                <FormControl>
                                    <div className="relative">
                                        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">$</span>
                                        <Input
                                            type="text"
                                            inputMode="decimal"
                                            className="pl-7"
                                            {...field}
                                            value={field.value ?? ""}
                                            onBlur={e => field.onChange(formatPriceDisplay(e.target.value))}
                                        />
                                    </div>
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                ) : (
                    <div className="space-y-4">
                        <div className="flex justify-between items-center">
                            <h4 className="font-semibold text-sm uppercase tracking-wider text-muted-foreground">Receta</h4>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => append({ source: "INGREDIENT", quantity: "", unitId: "" })}
                            >
                                <Plus className="h-4 w-4 mr-2" /> Agregar Elemento
                            </Button>
                        </div>

                        <div className="grid gap-4 border bg-muted/30 p-4 md:grid-cols-3">
                            <FormField
                                control={form.control}
                                name="yieldQty"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Rendimiento físico del lote</FormLabel>
                                        <FormControl>
                                            <Input
                                                type="number"
                                                step="0.01"
                                                {...field}
                                                value={field.value ?? ""}
                                                onChange={e => field.onChange(parseFloat(e.target.value))}
                                            />
                                        </FormControl>
                                        <FormDescription>
                                            Cantidad física total obtenida. Ej.: 1.500 g de carne preparada.
                                        </FormDescription>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="yieldUnitId"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Unidad física</FormLabel>
                                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                                            <FormControl>
                                                <SelectTrigger>
                                                    <SelectValue placeholder="Ej: Unidad o Plato" />
                                                </SelectTrigger>
                                            </FormControl>
                                            <SelectContent>
                                                {units.map((u) => (
                                                    <SelectItem key={u.id} value={u.id}>{u.name}</SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="sellableUnitsPerBatch"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Porciones vendibles</FormLabel>
                                        <FormControl>
                                            <Input
                                                type="number"
                                                min={1}
                                                step={1}
                                                {...field}
                                                value={field.value ?? ""}
                                                onChange={event => {
                                                    const value = event.target.value
                                                    field.onChange(value === "" ? undefined : Number(value))
                                                }}
                                            />
                                        </FormControl>
                                        <FormDescription>
                                            Unidades enteras que este lote permite vender.
                                        </FormDescription>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <div className="md:col-span-3 border-t pt-3 text-sm">
                                <span className="text-muted-foreground">Tamaño aproximado por porción: </span>
                                <span className="font-medium">
                                    {derivedPortionSize !== null
                                        ? `${derivedPortionSize.toLocaleString("es-CO", { maximumFractionDigits: 6 })} ${selectedYieldUnit?.abbreviation ?? ""}`
                                        : "Completa rendimiento y porciones"}
                                </span>
                                <p className="mt-1 text-xs text-muted-foreground">
                                    Este valor se deriva del rendimiento físico dividido entre las porciones; no se almacena por separado.
                                </p>
                            </div>
                        </div>

                        {fields.length === 0 ? (
                            <Card className="border-dashed">
                                <CardContent className="flex flex-col items-center justify-center p-6 text-center text-muted-foreground">
                                    <p className="text-sm">No hay ingredientes en la receta aún.</p>
                                    <p className="text-xs mt-1">El costo teórico se calculará automáticamente.</p>
                                </CardContent>
                            </Card>
                        ) : (
                            <div className="space-y-2">
                                {fields.map((field, index) => {
                                    const source = watchLines?.[index]?.source
                                    const unitType = getUnitTypeForLine(index)
                                    const availableUnits = unitType ? units.filter(u => u.type === unitType) : units

                                    return (
                                        <div key={field.id} className="grid grid-cols-[120px_1fr_90px_130px_36px] gap-2 items-end p-3 border rounded-lg bg-muted/20">
                                            {/* Source */}
                                            <FormField
                                                control={form.control}
                                                name={`lines.${index}.source`}
                                                render={({ field: f }) => (
                                                    <FormItem>
                                                        <FormLabel className="text-xs">Origen</FormLabel>
                                                        <Select
                                                            onValueChange={(val) => {
                                                                f.onChange(val)
                                                                // Reset item selection when type changes
                                                                form.setValue(`lines.${index}.masterIngredientId`, undefined)
                                                                form.setValue(`lines.${index}.childItemId`, undefined)
                                                                form.setValue(`lines.${index}.unitId`, "")
                                                            }}
                                                            defaultValue={f.value}
                                                        >
                                                            <FormControl>
                                                                <SelectTrigger className="h-8 text-xs"><SelectValue /></SelectTrigger>
                                                            </FormControl>
                                                            <SelectContent>
                                                                <SelectItem value="INGREDIENT">Insumo</SelectItem>
                                                                <SelectItem value="PREPARATION">Preparación</SelectItem>
                                                            </SelectContent>
                                                        </Select>
                                                    </FormItem>
                                                )}
                                            />

                                            {/* Item selector — ingredient or preparation combobox */}
                                            {source === "INGREDIENT" ? (
                                                <FormField
                                                    control={form.control}
                                                    name={`lines.${index}.masterIngredientId`}
                                                    render={({ field: f }) => (
                                                        <FormItem>
                                                            <FormLabel className="text-xs">Insumo</FormLabel>
                                                            <FormControl>
                                                                <ItemCombobox
                                                                    items={ingredientOptions}
                                                                    value={f.value}
                                                                    onChange={(id) => {
                                                                        f.onChange(id)
                                                                        // Reset unit when ingredient changes
                                                                        form.setValue(`lines.${index}.unitId`, "")
                                                                    }}
                                                                    placeholder="Buscar insumo..."
                                                                />
                                                            </FormControl>
                                                            <FormMessage />
                                                        </FormItem>
                                                    )}
                                                />
                                            ) : (
                                                <FormField
                                                    control={form.control}
                                                    name={`lines.${index}.childItemId`}
                                                    render={({ field: f }) => (
                                                        <FormItem>
                                                            <FormLabel className="text-xs">Preparación</FormLabel>
                                                            <FormControl>
                                                                <ItemCombobox
                                                                    items={preparations}
                                                                    value={f.value}
                                                                    onChange={(id) => {
                                                                        f.onChange(id)
                                                                        form.setValue(`lines.${index}.unitId`, "")
                                                                    }}
                                                                    placeholder="Buscar preparación..."
                                                                />
                                                            </FormControl>
                                                            <FormMessage />
                                                        </FormItem>
                                                    )}
                                                />
                                            )}

                                            {/* Quantity */}
                                            <FormField
                                                control={form.control}
                                                name={`lines.${index}.quantity`}
                                                render={({ field: f }) => (
                                                    <FormItem>
                                                        <FormLabel className="text-xs">Cant.</FormLabel>
                                                        <FormControl>
                                                            <Input
                                                                className="h-8 text-xs"
                                                                placeholder="Ej. 0,5"
                                                                {...f}
                                                                onBlur={e => f.onChange(formatPriceDisplay(e.target.value))}
                                                            />
                                                        </FormControl>
                                                    </FormItem>
                                                )}
                                            />

                                            {/* Unit — filtered by ingredient/preparation base unit type */}
                                            <FormField
                                                control={form.control}
                                                name={`lines.${index}.unitId`}
                                                render={({ field: f }) => (
                                                    <FormItem>
                                                        <FormLabel className="text-xs">Unidad</FormLabel>
                                                        <Select onValueChange={f.onChange} value={f.value}>
                                                            <FormControl>
                                                                <SelectTrigger className="h-8 text-xs">
                                                                    <SelectValue placeholder="Unidad" />
                                                                </SelectTrigger>
                                                            </FormControl>
                                                            <SelectContent>
                                                                {availableUnits.map((u) => (
                                                                    <SelectItem key={u.id} value={u.id}>{u.abbreviation}</SelectItem>
                                                                ))}
                                                            </SelectContent>
                                                        </Select>
                                                    </FormItem>
                                                )}
                                            />

                                            <Button type="button" variant="ghost" size="icon" className="h-8 w-8 text-destructive hover:bg-destructive/10" onClick={() => remove(index)}>
                                                <Trash2 className="h-4 w-4" />
                                            </Button>
                                        </div>
                                    )
                                })}
                            </div>
                        )}
                        {form.formState.errors.lines?.root && (
                            <p className="text-sm font-medium text-destructive">{form.formState.errors.lines.root.message}</p>
                        )}
                    </div>
                )}

                <div className="flex justify-end pt-4">
                    <Button type="submit" disabled={isPending || !form.formState.isValid}>
                        {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                        {isPending ? "Guardando..." : "Guardar Producto"}
                    </Button>
                </div>
            </form>
        </Form>
    )
}
