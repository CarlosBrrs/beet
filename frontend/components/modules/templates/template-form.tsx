"use client"

import { useForm, useFieldArray } from "react-hook-form"
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
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Loader2, Plus, Trash2, Box, Command, Info } from "lucide-react"
import { formatPriceDisplay, parsePriceInput } from "@/lib/formatters"
import { useCreateSubmenuTemplate } from "@/lib/hooks/use-submenu-nodes"
import { CreateTemplateRequest } from "@/lib/api-types"
import { useEffect, useState } from "react"
import { useTemplateOptionProducts } from "@/lib/hooks/use-items"
import { useCreateCatalogTemplate, useTemplate, useUpdateTemplate } from "@/lib/hooks/use-templates"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ItemResponse } from "@/lib/api-types"
import { SheetShell } from "@/components/shared/sheet-shell"
import { ProductForm } from "@/components/modules/products/product-form"

// ── Item Combobox for Templates ──
// Templates only reference active products explicitly enabled as options.

function TemplateItemCombobox({
    value,
    onChange,
    excludedProductIds,
}: {
    value?: string
    onChange: (v: string) => void
    excludedProductIds: string[]
}) {
    const { data: products = [] } = useTemplateOptionProducts()
    const [isCreateOpen, setIsCreateOpen] = useState(false)
    const [createdProduct, setCreatedProduct] = useState<ItemResponse | null>(null)
    const availableProducts = createdProduct && !products.some((product) => product.id === createdProduct.id)
        ? [createdProduct, ...products]
        : products

    return (
        <div className="flex gap-1">
            <Select value={value} onValueChange={onChange}>
                <SelectTrigger className="h-8 text-xs">
                    <SelectValue placeholder="Seleccionar producto..." />
                </SelectTrigger>
                <SelectContent>
                    {availableProducts.length === 0 ? (
                        <div className="p-2 text-xs text-muted-foreground text-center">No hay productos activos habilitados como opcion de armable</div>
                    ) : (
                        availableProducts.map((p: ItemResponse) => (
                            <SelectItem key={p.id} value={p.id} disabled={p.id !== value && excludedProductIds.includes(p.id)}>
                                <div className="flex items-center gap-2">
                                    <Box className="h-3 w-3 text-emerald-500" />
                                    {p.name}
                                </div>
                            </SelectItem>
                        ))
                    )}
                </SelectContent>
            </Select>
            <Button type="button" variant="outline" size="icon" className="h-8 w-8 shrink-0" title="Crear opcion" onClick={() => setIsCreateOpen(true)}>
                <Plus className="h-3 w-3" />
            </Button>
            <SheetShell open={isCreateOpen} onOpenChange={setIsCreateOpen} size="xl" title="Nueva opcion para armable" description="Crea un producto de catalogo no publicado y usalo inmediatamente en este slot.">
                <div className="mt-6">
                    <ProductForm forceAvailableAsTemplateOption onSuccess={(product) => {
                        setCreatedProduct(product)
                        onChange(product.id)
                        setIsCreateOpen(false)
                    }} />
                </div>
            </SheetShell>
        </div>
    )
}

// ── Zod Schema ──

const slotOptionSchema = z.object({
    itemId: z.string().min(1, "Selecciona un producto"),
    surcharge: z.string().optional(),
    isDefault: z.boolean(),
    maxQuantity: z.number().min(1, "Mínimo 1"),
})

const slotSchema = z.object({
    name: z.string().min(2, "Nombre requerido (Ej: Bebidas)"),
    minSelection: z.number().min(0, "Mínimo 0"),
    maxSelection: z.number().min(1, "Mínimo 1"),
    options: z.array(slotOptionSchema).min(1, "Debe tener al menos una opción"),
}).superRefine((data, ctx) => {
    if (data.minSelection > data.maxSelection) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Mínimo no puede ser mayor que máximo", path: ["minSelection"] })
    }
    const defaultCount = data.options.filter(o => o.isDefault).length
    if (defaultCount > data.maxSelection) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: `No puedes tener más de ${data.maxSelection} por defecto`, path: ["options"] })
    }
    if (new Set(data.options.map((option) => option.itemId)).size !== data.options.length) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: "No repitas un producto dentro del mismo grupo", path: ["options"] })
    }
    data.options.forEach((option, index) => {
        if (option.maxQuantity > data.maxSelection) {
            ctx.addIssue({ code: z.ZodIssueCode.custom, message: "No puede superar el máximo del grupo", path: ["options", index, "maxQuantity"] })
        }
    })
})

const templateSchema = z.object({
    name: z.string().min(2, "Mínimo 2 caracteres"),
    description: z.string().optional(),
    basePrice: z.string().min(1, "Precio base requerido"),
    slots: z.array(slotSchema).min(1, "Debe tener al menos una selección (Slot)"),
})

type TemplateFormValues = z.infer<typeof templateSchema>

// ── Components ──

interface TemplateFormProps {
    menuId?: string
    submenuId?: string
    templateId?: string | null
    onSuccess?: () => void
}

export function TemplateForm({ menuId, submenuId, templateId, onSuccess }: TemplateFormProps) {
    const createSubmenuTemplate = useCreateSubmenuTemplate(menuId ?? "", submenuId ?? "")
    const createCatalogTemplate = useCreateCatalogTemplate()
    const updateTemplate = useUpdateTemplate(templateId ?? "")
    const { data: initialTemplate, isLoading } = useTemplate(templateId)

    const form = useForm<TemplateFormValues>({
        resolver: zodResolver(templateSchema),
        mode: "onChange",
        defaultValues: {
            name: "",
            description: "",
            basePrice: "",
            slots: [],
        },
    })

    const { fields: slotFields, append: appendSlot, remove: removeSlot } = useFieldArray({
        control: form.control,
        name: "slots",
    })

    useEffect(() => {
        if (!initialTemplate) return
        form.reset({
            name: initialTemplate.name,
            description: initialTemplate.description ?? "",
            basePrice: initialTemplate.basePrice.toString(),
            slots: initialTemplate.slots.map((slot) => ({
                name: slot.name,
                minSelection: slot.minSelection,
                maxSelection: slot.maxSelection,
                options: slot.options.map((option) => ({
                    itemId: option.itemId,
                    surcharge: option.surcharge.toString(),
                    isDefault: option.isDefault,
                    maxQuantity: option.maxQuantity,
                })),
            })),
        })
    }, [form, initialTemplate])

    const isPending = createSubmenuTemplate.isPending || createCatalogTemplate.isPending || updateTemplate.isPending

    const onSubmit = (values: TemplateFormValues) => {
        const payload: CreateTemplateRequest = {
            name: values.name,
            description: values.description,
            basePrice: parsePriceInput(values.basePrice),
            slots: values.slots.map((s, idx) => ({
                name: s.name,
                minSelection: s.minSelection,
                maxSelection: s.maxSelection,
                sortOrder: idx,
                options: s.options.map((o, oIdx) => ({
                    itemId: o.itemId,
                    surcharge: o.surcharge ? parsePriceInput(o.surcharge) : 0,
                    isDefault: s.options.length === 1 && s.minSelection === 1 && s.maxSelection === 1
                        ? true
                        : o.isDefault,
                    maxQuantity: o.maxQuantity,
                    sortOrder: oIdx,
                }))
            }))
        }

        const mutation = templateId
            ? updateTemplate
            : menuId && submenuId
                ? createSubmenuTemplate
                : createCatalogTemplate
        mutation.mutate(payload, {
            onSuccess: () => onSuccess?.()
        })
    }

    if (isLoading) {
        return <div className="flex justify-center p-8"><Loader2 className="h-6 w-6 animate-spin" /></div>
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
                                <FormLabel>Nombre de la Plantilla</FormLabel>
                                <FormControl>
                                    <Input placeholder="Ej: Combo Ejecutivo" {...field} />
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
                                    <Textarea placeholder="Ej: Incluye plato fuerte y bebida..." className="resize-none" {...field} value={field.value ?? ""} />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="basePrice"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Precio Base</FormLabel>
                                <FormDescription>Precio del combo sin contar los recargos adicionales.</FormDescription>
                                <FormControl>
                                    <div className="relative w-1/2">
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
                </div>

                <div className="space-y-4 pt-4 border-t">
                    <div className="flex gap-3 border bg-muted/30 p-3 text-sm text-muted-foreground">
                        <Info className="mt-0.5 h-4 w-4 shrink-0" />
                        <p>
                            Cada slot agrupa productos seleccionables. Solo aparecen productos activos habilitados como opcion de armable. El boton + permite crear una opcion interna sin publicarla individualmente.
                        </p>
                    </div>
                    <div className="flex justify-between items-center">
                        <div>
                            <h4 className="font-semibold text-sm uppercase tracking-wider text-muted-foreground">Opciones (Slots)</h4>
                            <p className="text-xs text-muted-foreground">Grupos de opciones (Ej: Bebidas, Acompañamientos).</p>
                        </div>
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => appendSlot({ name: "", minSelection: 1, maxSelection: 1, options: [{ itemId: "", isDefault: true, surcharge: "", maxQuantity: 1 }] })}
                        >
                            <Plus className="h-4 w-4 mr-2" /> Agregar Grupo
                        </Button>
                    </div>

                    {slotFields.length === 0 ? (
                        <Card className="border-dashed bg-muted/20">
                            <CardContent className="flex flex-col items-center justify-center p-8 text-center text-muted-foreground">
                                <Command className="h-8 w-8 mb-2 opacity-50" />
                                <p className="text-sm">Agrega grupos de opciones para tu plantilla.</p>
                            </CardContent>
                        </Card>
                    ) : (
                        <div className="space-y-6">
                            {slotFields.map((slotField, slotIndex) => (
                                <SlotFieldBuilder
                                    key={slotField.id}
                                    form={form}
                                    slotIndex={slotIndex}
                                    onRemove={() => removeSlot(slotIndex)}
                                />
                            ))}
                        </div>
                    )}
                    {form.formState.errors.slots?.root && (
                        <p className="text-sm font-medium text-destructive">{form.formState.errors.slots.root.message}</p>
                    )}
                </div>

                <div className="flex justify-end pt-4">
                    <Button type="submit" disabled={isPending || !form.formState.isValid}>
                        {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                        {isPending ? "Guardando..." : "Guardar Plantilla"}
                    </Button>
                </div>
            </form>
        </Form>
    )
}

import { UseFormReturn } from "react-hook-form"

function SlotFieldBuilder({ form, slotIndex, onRemove }: { form: UseFormReturn<TemplateFormValues>; slotIndex: number; onRemove: () => void }) {
    const { fields: optionFields, append: appendOption, remove: removeOption } = useFieldArray({
        control: form.control,
        name: `slots.${slotIndex}.options`,
    })
    const slot = form.watch(`slots.${slotIndex}`)
    const isFixed = slot?.options.length === 1 && slot.minSelection === 1 && slot.maxSelection === 1
    const selectedProductIds = slot?.options.map((option) => option.itemId).filter(Boolean) ?? []

    return (
        <Card className="shadow-sm">
            <CardHeader className="py-3 px-4 bg-muted/40 flex flex-row items-center justify-between border-b">
                <CardTitle className="text-sm font-medium flex-1">
                    <FormField
                        control={form.control}
                        name={`slots.${slotIndex}.name`}
                        render={({ field }) => (
                            <FormItem className="w-1/2">
                                <FormControl>
                                    <Input placeholder="Ej: Selecciona tu bebida" className="h-8 shadow-none" {...field} />
                                </FormControl>
                            </FormItem>
                        )}
                    />
                </CardTitle>
                <div className="flex items-center gap-3">
                    <div className="flex items-center gap-2">
                        <span className="text-xs text-muted-foreground font-medium">Min:</span>
                        <FormField
                            control={form.control}
                            name={`slots.${slotIndex}.minSelection`}
                            render={({ field }) => (
                                <FormItem>
                                    <FormControl>
                                        <Input
                                            type="number"
                                            min={0}
                                            max={slot?.maxSelection ?? 1}
                                            aria-invalid={!!form.formState.errors.slots?.[slotIndex]?.minSelection}
                                            className="h-8 w-14 p-1 text-center"
                                            {...field}
                                            onChange={(event) => {
                                                const nextValue = Number(event.target.value)
                                                field.onChange(Math.min(Math.max(Number.isFinite(nextValue) ? nextValue : 0, 0), slot?.maxSelection ?? 1))
                                            }}
                                        />
                                    </FormControl>
                                    <FormMessage className="max-w-28 text-[10px]" />
                                </FormItem>
                            )}
                        />
                    </div>
                    <div className="flex items-center gap-2">
                        <span className="text-xs text-muted-foreground font-medium">Max:</span>
                        <FormField
                            control={form.control}
                            name={`slots.${slotIndex}.maxSelection`}
                            render={({ field }) => (
                                <FormItem>
                                    <FormControl>
                                        <Input
                                            type="number"
                                            min={Math.max(slot?.minSelection ?? 0, 1)}
                                            aria-invalid={!!form.formState.errors.slots?.[slotIndex]?.maxSelection}
                                            className="h-8 w-14 p-1 text-center"
                                            {...field}
                                            onChange={(event) => {
                                                const nextValue = Number(event.target.value)
                                                const maxSelection = Math.max(Number.isFinite(nextValue) ? nextValue : 1, slot?.minSelection ?? 1, 1)
                                                field.onChange(maxSelection)
                                                slot?.options.forEach((option, optionIndex) => {
                                                    if (option.maxQuantity > maxSelection) {
                                                        form.setValue(`slots.${slotIndex}.options.${optionIndex}.maxQuantity`, maxSelection, { shouldValidate: true })
                                                    }
                                                })
                                            }}
                                        />
                                    </FormControl>
                                    <FormMessage className="max-w-28 text-[10px]" />
                                </FormItem>
                            )}
                        />
                    </div>
                    <Button type="button" variant="ghost" size="icon" className="h-8 w-8 text-destructive hover:bg-destructive/10" onClick={onRemove}>
                        <Trash2 className="h-4 w-4" />
                    </Button>
                </div>
            </CardHeader>
            <CardContent className="p-4 space-y-3">
                <p className="text-xs text-muted-foreground">
                    Min y max cuentan unidades totales elegidas. Si el slot tiene una unica opcion y min=max=1, queda fija y predeterminada.
                </p>
                {form.formState.errors.slots?.[slotIndex]?.options?.root && (
                    <p className="text-xs text-destructive">{form.formState.errors.slots[slotIndex].options.root.message}</p>
                )}
                {form.formState.errors.slots?.[slotIndex]?.minSelection && (
                    <p className="text-xs text-destructive">{form.formState.errors.slots[slotIndex].minSelection.message}</p>
                )}
                {optionFields.length > 0 && (
                    <div className="grid grid-cols-[1fr_120px_76px_100px_36px] gap-3 px-2 text-[11px] font-medium text-muted-foreground">
                        <span>Producto</span>
                        <span>Cantidad maxima</span>
                        <span>Recargo</span>
                        <span>Predeterminado</span>
                        <span />
                    </div>
                )}
                {optionFields.map((optField, optIndex) => (
                    <div key={optField.id} className="grid grid-cols-[1fr_120px_76px_100px_36px] gap-3 items-center p-2 border rounded bg-background">
                        <FormField
                            control={form.control}
                            name={`slots.${slotIndex}.options.${optIndex}.itemId`}
                            render={({ field }) => (
                                <FormItem>
                                    <TemplateItemCombobox value={field.value} onChange={field.onChange} excludedProductIds={selectedProductIds} />
                                    <FormMessage className="text-[10px]" />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name={`slots.${slotIndex}.options.${optIndex}.maxQuantity`}
                            render={({ field }) => (
                                <FormItem>
                                    <FormControl>
                                        <Input
                                            type="number"
                                            min={1}
                                            max={slot?.maxSelection ?? 1}
                                            disabled={isFixed}
                                            aria-invalid={!!form.formState.errors.slots?.[slotIndex]?.options?.[optIndex]?.maxQuantity}
                                            className="h-8 text-xs"
                                            aria-label="Cantidad máxima"
                                            {...field}
                                            onChange={(event) => {
                                                const nextValue = Number(event.target.value)
                                                field.onChange(Math.min(Math.max(Number.isFinite(nextValue) ? nextValue : 1, 1), slot?.maxSelection ?? 1))
                                            }}
                                        />
                                    </FormControl>
                                    <FormMessage className="text-[10px]" />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name={`slots.${slotIndex}.options.${optIndex}.surcharge`}
                            render={({ field }) => (
                                <FormItem>
                                    <div className="relative">
                                        <span className="absolute left-2 top-1/2 -translate-y-1/2 text-muted-foreground text-xs">$</span>
                                        <FormControl>
                                            <Input
                                                placeholder="Recargo"
                                                className="h-8 pl-5 text-xs"
                                                {...field}
                                                value={field.value ?? ""}
                                                onBlur={e => field.onChange(formatPriceDisplay(e.target.value))}
                                            />
                                        </FormControl>
                                    </div>
                                    <FormMessage className="text-[10px]" />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name={`slots.${slotIndex}.options.${optIndex}.isDefault`}
                            render={({ field }) => (
                                <FormItem className="flex items-center gap-2 space-y-0 text-xs text-muted-foreground px-2">
                                    <FormControl>
                                        <input type="checkbox" className="h-3 w-3" checked={isFixed || field.value} disabled={isFixed} onChange={e => field.onChange(e.target.checked)} />
                                    </FormControl>
                                    <FormLabel className="font-normal text-xs cursor-pointer">Default</FormLabel>
                                </FormItem>
                            )}
                        />
                        <Button type="button" variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-destructive" onClick={() => removeOption(optIndex)}>
                            <Trash2 className="h-3 w-3" />
                        </Button>
                    </div>
                ))}

                <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="w-full text-xs text-muted-foreground border border-dashed rounded"
                    onClick={() => appendOption({ itemId: "", isDefault: false, surcharge: "", maxQuantity: 1 })}
                >
                    <Plus className="h-3 w-3 mr-2" /> Agregar Opción
                </Button>
            </CardContent>
        </Card>
    )
}
