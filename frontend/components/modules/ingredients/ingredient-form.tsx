"use client"

import { useCallback, useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Button } from "@/components/ui/button"
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card"
import { CreateIngredientRequest } from "@/lib/api-types"
import { useUnits } from "@/lib/hooks/use-units"
import { useMockSuppliers } from "@/lib/hooks/use-ingredients"
import { Loader2, Plus, Search, HelpCircle, X } from "lucide-react"
import { Separator } from "@/components/ui/separator"
import { formatPriceDisplay, parsePriceInput, formatNumber } from "@/lib/formatters"

// ── Zod Schema ──

const formSchema = z.object({
    // Ingredient
    name: z.string().min(2, "Ingredient name must be at least 2 characters."),
    baseUnitId: z.string().min(1, "Please select a base unit."),

    // Supplier
    supplierMode: z.enum(["existing", "new"]),
    existingSupplierId: z.string().optional(),
    supplierName: z.string().optional(),
    documentTypeId: z.string().optional(),
    documentNumber: z.string().optional(),

    // Purchase Info (reordered for natural reading)
    purchaseUnitName: z.string().min(1, "Purchase unit name is required."),
    conversionFactor: z.number().positive("Must be greater than 0."),
    conversionUnitId: z.string().min(1, "Please select a conversion unit."),
    totalPrice: z.number().positive("Must be greater than 0."),
    brandName: z.string().optional(),
}).superRefine((data, ctx) => {
    if (data.supplierMode === "existing" && !data.existingSupplierId) {
        ctx.addIssue({
            code: z.ZodIssueCode.custom,
            message: "Please select a supplier.",
            path: ["existingSupplierId"],
        })
    }
    if (data.supplierMode === "new") {
        if (!data.supplierName || data.supplierName.length < 2) {
            ctx.addIssue({
                code: z.ZodIssueCode.custom,
                message: "Supplier name must be at least 2 characters.",
                path: ["supplierName"],
            })
        }
        if (!data.documentNumber || data.documentNumber.length < 3) {
            ctx.addIssue({
                code: z.ZodIssueCode.custom,
                message: "Document number is required.",
                path: ["documentNumber"],
            })
        }
    }
})

type FormValues = z.infer<typeof formSchema>

// ── Mock Document Types ──

const MOCK_DOCUMENT_TYPES = [
    { id: "dt-nit", name: "NIT" },
    { id: "dt-cc", name: "CC" },
    { id: "dt-ce", name: "CE" },
    { id: "dt-rfc", name: "RFC" },
]


// ── Help Guide Component ──

function PurchaseHelpGuide({ onClose }: { onClose: () => void }) {
    return (
        <Card size="sm" className="mb-4">
            <CardHeader>
                <div className="flex items-center justify-between">
                    <CardTitle>📦 ¿Cómo llenar la info de compra?</CardTitle>
                    <button
                        onClick={onClose}
                        className="text-muted-foreground hover:text-foreground transition-colors cursor-pointer p-1"
                    >
                        <X className="h-4 w-4" />
                    </button>
                </div>
                <CardDescription>
                    Piensa en cómo te llega el producto del proveedor.
                </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
                <div className="rounded-md border bg-muted/40 p-3 space-y-1.5">
                    <p className="text-xs font-semibold text-amber-600">🟤 Bulto de Harina (base: Gramo)</p>
                    <div className="grid grid-cols-4 gap-2 text-xs">
                        <div><span className="text-muted-foreground">Empaque:</span><p className="font-mono">Bulto</p></div>
                        <div><span className="text-muted-foreground">Cantidad:</span><p className="font-mono">25</p></div>
                        <div><span className="text-muted-foreground">Unidad:</span><p className="font-mono">Kilogram</p></div>
                        <div><span className="text-muted-foreground">Precio:</span><p className="font-mono">$45.000</p></div>
                    </div>
                    <p className="text-[10px] italic text-muted-foreground mt-1">→ "1 Bulto de 25 Kilogramos me cuesta $45.000" · Costo: $1,80/g</p>
                </div>

                <div className="rounded-md border bg-muted/40 p-3 space-y-1.5">
                    <p className="text-xs font-semibold text-amber-600">🟤 Compra por Kilo (base: Gramo)</p>
                    <div className="grid grid-cols-4 gap-2 text-xs">
                        <div><span className="text-muted-foreground">Empaque:</span><p className="font-mono">Kilogramo</p></div>
                        <div><span className="text-muted-foreground">Cantidad:</span><p className="font-mono">1</p></div>
                        <div><span className="text-muted-foreground">Unidad:</span><p className="font-mono">Kilogram</p></div>
                        <div><span className="text-muted-foreground">Precio:</span><p className="font-mono">$12.000</p></div>
                    </div>
                    <p className="text-[10px] italic text-muted-foreground mt-1">→ "1 Kilogramo de 1 Kilogramo me cuesta $12.000" · Costo: $12,00/g</p>
                </div>

                <div className="rounded-md border bg-muted/40 p-3 space-y-1.5">
                    <p className="text-xs font-semibold text-blue-600">🔵 Pimpina de Aceite (base: Mililitro)</p>
                    <div className="grid grid-cols-4 gap-2 text-xs">
                        <div><span className="text-muted-foreground">Empaque:</span><p className="font-mono">Pimpina</p></div>
                        <div><span className="text-muted-foreground">Cantidad:</span><p className="font-mono">20</p></div>
                        <div><span className="text-muted-foreground">Unidad:</span><p className="font-mono">Liter</p></div>
                        <div><span className="text-muted-foreground">Precio:</span><p className="font-mono">$60.000</p></div>
                    </div>
                    <p className="text-[10px] italic text-muted-foreground mt-1">→ "1 Pimpina de 20 Litros me cuesta $60.000" · Costo: $3,00/ml</p>
                </div>

                <div className="rounded-md border bg-muted/40 p-3 space-y-1.5">
                    <p className="text-xs font-semibold text-green-600">🟢 Caja de Huevos (base: Pieza)</p>
                    <div className="grid grid-cols-4 gap-2 text-xs">
                        <div><span className="text-muted-foreground">Empaque:</span><p className="font-mono">Caja</p></div>
                        <div><span className="text-muted-foreground">Cantidad:</span><p className="font-mono">30</p></div>
                        <div><span className="text-muted-foreground">Unidad:</span><p className="font-mono">Piece</p></div>
                        <div><span className="text-muted-foreground">Precio:</span><p className="font-mono">$15.000</p></div>
                    </div>
                    <p className="text-[10px] italic text-muted-foreground mt-1">→ "1 Caja de 30 Piezas me cuesta $15.000" · Costo: $500/pcs</p>
                </div>

                <div className="rounded-md border bg-muted/40 p-3 space-y-1.5">
                    <p className="text-xs font-semibold text-green-600">🟢 Compra por Unidad (base: Pieza)</p>
                    <div className="grid grid-cols-4 gap-2 text-xs">
                        <div><span className="text-muted-foreground">Empaque:</span><p className="font-mono">Unidad</p></div>
                        <div><span className="text-muted-foreground">Cantidad:</span><p className="font-mono">1</p></div>
                        <div><span className="text-muted-foreground">Unidad:</span><p className="font-mono">Piece</p></div>
                        <div><span className="text-muted-foreground">Precio:</span><p className="font-mono">$2.500</p></div>
                    </div>
                    <p className="text-[10px] italic text-muted-foreground mt-1">→ "1 Unidad de 1 Pieza me cuesta $2.500" · Costo: $2.500/pcs</p>
                </div>
            </CardContent>
        </Card>
    )
}

// ── Main Form ──

interface IngredientFormProps {
    onSubmit: (values: CreateIngredientRequest) => void
    isSubmitting?: boolean
}

export function IngredientForm({ onSubmit, isSubmitting }: IngredientFormProps) {
    const { data: units, isLoading: unitsLoading } = useUnits()
    const mockSuppliers = useMockSuppliers()
    const [showHelp, setShowHelp] = useState(false)
    const [priceDisplay, setPriceDisplay] = useState("")

    const form = useForm<FormValues>({
        resolver: zodResolver(formSchema),
        mode: "onChange",
        defaultValues: {
            name: "",
            baseUnitId: "",
            supplierMode: "new",
            existingSupplierId: "",
            supplierName: "",
            documentTypeId: MOCK_DOCUMENT_TYPES[0].id,
            documentNumber: "",
            purchaseUnitName: "",
            conversionFactor: 0,
            conversionUnitId: "",
            totalPrice: 0,
            brandName: "",
        },
    })

    const watchBaseUnitId = form.watch("baseUnitId")
    const watchConversionFactor = form.watch("conversionFactor")
    const watchConversionUnitId = form.watch("conversionUnitId")
    const watchTotalPrice = form.watch("totalPrice")
    const watchSupplierMode = form.watch("supplierMode")
    const watchName = form.watch("name")

    // Filter: base units (isBase: true)
    const baseUnits = useMemo(() => {
        return (units ?? []).filter(u => u.isBase)
    }, [units])

    // Filter: conversion units (same type as selected base unit)
    const conversionUnits = useMemo(() => {
        if (!watchBaseUnitId || !units) return []
        const baseUnit = units.find(u => u.id === watchBaseUnitId)
        if (!baseUnit) return []
        return units.filter(u => u.type === baseUnit.type)
    }, [watchBaseUnitId, units])

    // Cost per base unit (always computed, defaults to 0)
    const costInfo = useMemo(() => {
        const baseUnit = units?.find(u => u.id === watchBaseUnitId)
        const abbr = baseUnit?.abbreviation ?? "—"
        const ingredientLabel = watchName?.trim() || "..."

        if (!watchConversionFactor || !watchConversionUnitId || !watchTotalPrice || !units) {
            return { costPerBaseUnit: "0,00", baseAbbr: abbr, ingredientLabel }
        }
        const convUnit = units.find(u => u.id === watchConversionUnitId)
        if (!convUnit) return { costPerBaseUnit: "0,00", baseAbbr: abbr, ingredientLabel }

        const finalFactor = watchConversionFactor * convUnit.factorToBase
        const costPerBaseUnit = watchTotalPrice / finalFactor

        return {
            costPerBaseUnit: formatNumber(costPerBaseUnit),
            baseAbbr: abbr,
            ingredientLabel,
        }
    }, [watchConversionFactor, watchConversionUnitId, watchTotalPrice, watchBaseUnitId, watchName, units])

    // Handle price input with live thousands formatting
    const handlePriceChange = useCallback((rawValue: string) => {
        const formatted = formatPriceDisplay(rawValue)
        setPriceDisplay(formatted)
        form.setValue("totalPrice", parsePriceInput(formatted), { shouldValidate: true })
    }, [form])

    // Build the real payload
    const handleFormSubmit = (values: FormValues) => {
        const selectedSupplier = values.supplierMode === "existing"
            ? mockSuppliers.find(s => s.id === values.existingSupplierId)
            : null

        const payload: CreateIngredientRequest = {
            masterIngredient: {
                name: values.name,
                baseUnitId: values.baseUnitId,
            },
            supplier: values.supplierMode === "existing" && selectedSupplier
                ? {
                    id: selectedSupplier.id,
                    name: selectedSupplier.name,
                    documentTypeId: selectedSupplier.documentTypeId,
                    documentNumber: selectedSupplier.documentNumber,
                }
                : {
                    id: null,
                    name: values.supplierName,
                    documentTypeId: values.documentTypeId,
                    documentNumber: values.documentNumber,
                },
            supplierItem: {
                brandName: values.brandName || undefined,
                purchaseUnitName: values.purchaseUnitName,
                conversionFactor: values.conversionFactor,
                conversionUnitId: values.conversionUnitId,
                totalPrice: values.totalPrice,
            },
        }
        onSubmit(payload)
    }

    if (unitsLoading) {
        return (
            <div className="flex items-center justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                <span className="ml-2 text-muted-foreground">Loading units...</span>
            </div>
        )
    }

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(handleFormSubmit)} className="space-y-6">

                {/* ── Section 1: Ingredient ── */}
                <div>
                    <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-3">Ingredient</h3>
                    <div className="space-y-4">
                        <FormField
                            control={form.control}
                            name="name"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>
                                    <FormControl>
                                        <Input placeholder="e.g. Harina de Trigo" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="baseUnitId"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Base Unit</FormLabel>
                                    <Select onValueChange={(val) => {
                                        field.onChange(val)
                                        form.setValue("conversionUnitId", "", { shouldValidate: true })
                                        form.trigger("conversionUnitId")
                                    }} value={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select base unit" />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            {baseUnits.map(u => (
                                                <SelectItem key={u.id} value={u.id}>
                                                    {u.name} ({u.abbreviation})
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    </div>
                </div>

                <Separator />

                {/* ── Section 2: Supplier ── */}
                <div>
                    <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-3">Supplier</h3>

                    <div className="flex gap-2 mb-4">
                        <Button
                            type="button"
                            size="sm"
                            variant={watchSupplierMode === "existing" ? "default" : "outline"}
                            onClick={() => {
                                form.setValue("supplierMode", "existing", { shouldValidate: true })
                                // Re-validate the field that matters in this mode
                                form.trigger("existingSupplierId")
                            }}
                        >
                            <Search className="mr-1 h-3.5 w-3.5" /> Existing
                        </Button>
                        <Button
                            type="button"
                            size="sm"
                            variant={watchSupplierMode === "new" ? "default" : "outline"}
                            onClick={() => {
                                form.setValue("supplierMode", "new", { shouldValidate: true })
                                // Re-validate the fields that matter in this mode
                                form.trigger(["supplierName", "documentNumber"])
                            }}
                        >
                            <Plus className="mr-1 h-3.5 w-3.5" /> Quick Add
                        </Button>
                    </div>

                    {watchSupplierMode === "existing" ? (
                        <FormField
                            control={form.control}
                            name="existingSupplierId"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Select Supplier</FormLabel>
                                    <Select onValueChange={field.onChange} value={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Choose a supplier" />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            {mockSuppliers.map(s => (
                                                <SelectItem key={s.id} value={s.id}>
                                                    {s.name} ({s.documentNumber})
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    ) : (
                        <div className="space-y-4">
                            <FormField
                                control={form.control}
                                name="supplierName"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Supplier Name</FormLabel>
                                        <FormControl>
                                            <Input placeholder="e.g. Molinos del Sur" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <div className="grid grid-cols-2 gap-3">
                                <FormField
                                    control={form.control}
                                    name="documentTypeId"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>Doc Type</FormLabel>
                                            <Select onValueChange={field.onChange} value={field.value}>
                                                <FormControl>
                                                    <SelectTrigger>
                                                        <SelectValue placeholder="Type" />
                                                    </SelectTrigger>
                                                </FormControl>
                                                <SelectContent>
                                                    {MOCK_DOCUMENT_TYPES.map(dt => (
                                                        <SelectItem key={dt.id} value={dt.id}>
                                                            {dt.name}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                <FormField
                                    control={form.control}
                                    name="documentNumber"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>Doc Number</FormLabel>
                                            <FormControl>
                                                <Input placeholder="e.g. 900-123-456" {...field} />
                                            </FormControl>
                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />
                            </div>
                        </div>
                    )}
                </div>

                <Separator />

                {/* ── Section 3: Purchase Info ── */}
                <div>
                    <div className="flex items-center justify-between mb-3">
                        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">Purchase Info</h3>
                        <button
                            type="button"
                            onClick={() => setShowHelp(!showHelp)}
                            className="text-muted-foreground hover:text-foreground transition-colors flex items-center gap-1 text-xs cursor-pointer"
                        >
                            <HelpCircle className="h-4 w-4" />
                            <span>{showHelp ? "Hide guide" : "How to fill"}</span>
                        </button>
                    </div>

                    {showHelp && <PurchaseHelpGuide onClose={() => setShowHelp(false)} />}

                    {/* Description to orient the user */}
                    <p className="text-xs text-muted-foreground mb-4 italic">
                        Léelo como: &quot;1 [empaque] de [cantidad] [unidades] me cuesta $[precio]&quot;
                    </p>

                    <div className="space-y-4">
                        {/* Row 1: Package Name + Qty */}
                        <div className="grid grid-cols-2 gap-3">
                            <FormField
                                control={form.control}
                                name="purchaseUnitName"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Package Name</FormLabel>
                                        <FormControl>
                                            <Input placeholder="Bulto, Caja, Kg..." {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="conversionFactor"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Quantity per Package</FormLabel>
                                        <FormControl>
                                            <Input
                                                type="number"
                                                step="0.001"
                                                placeholder="25"
                                                {...field}
                                                onChange={e => field.onChange(parseFloat(e.target.value) || 0)}
                                            />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        {/* Row 2: Conversion Unit + Total Price */}
                        <div className="grid grid-cols-2 gap-3">
                            <FormField
                                control={form.control}
                                name="conversionUnitId"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Unit</FormLabel>
                                        <Select
                                            onValueChange={field.onChange}
                                            value={field.value}
                                            disabled={!watchBaseUnitId}
                                        >
                                            <FormControl>
                                                <SelectTrigger>
                                                    <SelectValue placeholder={watchBaseUnitId ? "Select unit" : "Pick base unit first"} />
                                                </SelectTrigger>
                                            </FormControl>
                                            <SelectContent>
                                                {conversionUnits.map(u => (
                                                    <SelectItem key={u.id} value={u.id}>
                                                        {u.name} ({u.abbreviation})
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="totalPrice"
                                render={() => (
                                    <FormItem>
                                        <FormLabel>Total Price</FormLabel>
                                        <FormControl>
                                            <div className="relative">
                                                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">$</span>
                                                <Input
                                                    type="text"
                                                    inputMode="decimal"
                                                    placeholder="45.000,00"
                                                    className="pl-7"
                                                    value={priceDisplay}
                                                    onChange={e => handlePriceChange(e.target.value)}
                                                />
                                            </div>
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        {/* Cost per base unit — always visible info field */}
                        <div className="rounded-md border bg-muted/40 px-4 py-3 flex items-center justify-between">
                            <span className="text-sm text-muted-foreground">Costo por unidad base</span>
                            <span className="text-sm font-semibold text-emerald-600 font-mono">
                                1 {costInfo.baseAbbr} de {costInfo.ingredientLabel} = ${costInfo.costPerBaseUnit}
                            </span>
                        </div>

                        {/* Brand (optional) */}
                        <FormField
                            control={form.control}
                            name="brandName"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Brand <span className="text-muted-foreground text-xs">(optional)</span></FormLabel>
                                    <FormControl>
                                        <Input placeholder="e.g. Harina Especial Panadería" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                    </div>
                </div>

                <div className="flex justify-end pt-2">
                    <Button type="submit" disabled={isSubmitting || !form.formState.isValid}>
                        {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                        {isSubmitting ? "Creating..." : "Create Ingredient"}
                    </Button>
                </div>
            </form>
        </Form>
    )
}
