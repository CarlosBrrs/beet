"use client"

import * as React from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Button } from "@/components/ui/button";
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { Check, ChevronsUpDown, Loader2, Plus, Trash2 } from "lucide-react";
import { useCreatePreparation, useUpdateItem } from "@/lib/hooks/use-items";
import { useUnits } from "@/lib/hooks/use-units";
import { ItemResponse } from "@/lib/api-types";
import { parsePriceInput, formatPriceDisplay } from "@/lib/formatters";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command";
import { cn } from "@/lib/utils";
import { useInventoryStocks } from "@/lib/hooks/use-inventory";
import { useRestaurantContext } from "@/components/providers/restaurant-provider";

function IngredientCombobox({
    items,
    value,
    onChange,
}: {
    items: { id: string; name: string }[];
    value?: string;
    onChange: (id: string) => void;
}) {
    const [open, setOpen] = React.useState(false);
    const selectedItem = items.find((item) => item.id === value);

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <Button
                    variant="outline"
                    role="combobox"
                    aria-expanded={open}
                    className="w-full justify-between h-8 text-xs font-normal px-2"
                >
                    <span className="truncate">
                        {selectedItem ? selectedItem.name : "Buscar insumo..."}
                    </span>
                    <ChevronsUpDown className="ml-2 h-3 w-3 shrink-0 opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-[300px] p-0" align="start">
                <Command filter={(value, search) => {
                    if (value.toLowerCase().includes(search.toLowerCase())) return 1;
                    return 0;
                }}>
                    <CommandInput placeholder="Buscar insumo..." className="h-9 text-xs" />
                    <CommandList>
                        <CommandEmpty>No se encontraron resultados.</CommandEmpty>
                        <CommandGroup>
                            {items.map((item) => (
                                <CommandItem
                                    key={item.id}
                                    value={item.name}
                                    onSelect={() => {
                                        onChange(item.id);
                                        setOpen(false);
                                    }}
                                    className="text-xs"
                                >
                                    <Check
                                        className={cn(
                                            "mr-2 h-4 w-4",
                                            value === item.id ? "opacity-100" : "opacity-0"
                                        )}
                                    />
                                    {item.name}
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}

// ── Zod Schema ──

const recipeLineSchema = z.object({
    masterIngredientId: z.string().min(1, "Selecciona un insumo"),
    quantity: z.string().min(1, "Requerido"),
    unitId: z.string().min(1, "Requerido"),
});

const preparationSchema = z.object({
    name: z.string().min(2, "Mínimo 2 caracteres"),
    description: z.string().optional(),
    yieldQty: z.string().min(1, "Requerido"),
    yieldUnitId: z.string().min(1, "Selecciona una unidad"),
    lines: z.array(recipeLineSchema).min(1, "Agrega al menos un ingrediente"),
});

type PreparationFormValues = z.infer<typeof preparationSchema>;

// ── Props ──

interface PreparationFormProps {
    initialData?: ItemResponse;
    onSuccess?: () => void;
}

// ── Component ──

export function PreparationForm({ initialData, onSuccess }: PreparationFormProps) {
    const { restaurantId } = useRestaurantContext();
    const createPreparation = useCreatePreparation();
    const updateItem = useUpdateItem();
    const { data: units = [] } = useUnits();

    // Fetch only ingredients activated in this restaurant's inventory
    const { data: inventoryData } = useInventoryStocks({
        restaurantId: restaurantId!,
        page: 0,
        size: 200
    });
    const ingredients = inventoryData?.content ?? [];
    const ingredientOptions = ingredients.map(i => ({
        id: i.masterIngredientId,
        name: i.ingredientName
    }));

    const form = useForm<PreparationFormValues>({
        resolver: zodResolver(preparationSchema),
        mode: "onChange",
        defaultValues: {
            name: initialData?.name ?? "",
            description: initialData?.description ?? "",
            yieldQty: initialData?.yieldQty?.toString() ?? "",
            yieldUnitId: initialData?.yieldUnitId ?? "",
            lines: initialData?.recipeLines.map((l) => ({
                masterIngredientId: l.masterIngredientId ?? "",
                quantity: l.quantity.toString(),
                unitId: l.unitId,
            })) ?? [{ masterIngredientId: "", quantity: "", unitId: "" }],
        },
    });

    const { fields, append, remove } = useFieldArray({
        control: form.control,
        name: "lines",
    });

    const watchLines = form.watch("lines");

    // Filter units to match the base unit type of the selected ingredient
    const getUnitTypeForLine = (index: number) => {
        const line = watchLines[index];
        if (!line?.masterIngredientId) return null;
        const ingredient = ingredients.find(i => i.masterIngredientId === line.masterIngredientId);
        if (!ingredient?.unitAbbreviation) return null;
        const unit = units.find(u => u.abbreviation === ingredient.unitAbbreviation);
        return unit?.type ?? null;
    };

    const isPending = createPreparation.isPending || updateItem.isPending;

    const onSubmit = (values: PreparationFormValues) => {
        const lines = values.lines.map((l) => ({
            masterIngredientId: l.masterIngredientId,
            quantity: parsePriceInput(l.quantity),
            unitId: l.unitId,
        }));

        if (initialData) {
            updateItem.mutate(
                {
                    id: initialData.id,
                    data: {
                        name: values.name,
                        description: values.description,
                        yieldQty: parsePriceInput(values.yieldQty),
                        yieldUnitId: values.yieldUnitId,
                        lines,
                    },
                },
                { onSuccess: () => onSuccess?.() }
            );
        } else {
            createPreparation.mutate(
                {
                    name: values.name,
                    description: values.description,
                    yieldQty: parsePriceInput(values.yieldQty),
                    yieldUnitId: values.yieldUnitId,
                    lines,
                },
                { onSuccess: () => onSuccess?.() }
            );
        }
    };

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
                {/* Name */}
                <FormField
                    control={form.control}
                    name="name"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Nombre</FormLabel>
                            <FormControl>
                                <Input placeholder="Ej: Salsa de tomate, Masa de pizza..." {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                {/* Description */}
                <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Descripción (opcional)</FormLabel>
                            <FormControl>
                                <Textarea
                                    placeholder="Describe brevemente esta preparación"
                                    className="resize-none h-20"
                                    {...field}
                                    value={field.value ?? ""}
                                />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                {/* Yield */}
                <div className="grid grid-cols-2 gap-4">
                    <FormField
                        control={form.control}
                        name="yieldQty"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Rendimiento</FormLabel>
                                <FormControl>
                                    <Input
                                        placeholder="Ej: 1.5"
                                        {...field}
                                        onBlur={(e) => {
                                            field.onChange(formatPriceDisplay(e.target.value));
                                        }}
                                    />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                    <FormField
                        control={form.control}
                        name="yieldUnitId"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Unidad de rendimiento</FormLabel>
                                <Select onValueChange={field.onChange} defaultValue={field.value}>
                                    <FormControl>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Seleccionar..." />
                                        </SelectTrigger>
                                    </FormControl>
                                    <SelectContent>
                                        {units.map((u) => (
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

                {/* Recipe Lines */}
                <div className="space-y-3">
                    <div className="flex items-center justify-between">
                        <FormLabel>Ingredientes</FormLabel>
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() =>
                                append({ masterIngredientId: "", quantity: "", unitId: "" })
                            }
                        >
                            <Plus className="h-3.5 w-3.5 mr-1" /> Agregar línea
                        </Button>
                    </div>

                    {fields.map((field, index) => (
                        <div
                            key={field.id}
                            className="grid grid-cols-[1fr_100px_140px_36px] gap-3 items-end p-3 border rounded-lg bg-muted/20"
                        >
                            {/* Ingredient selector */}
                            <FormField
                                control={form.control}
                                name={`lines.${index}.masterIngredientId`}
                                render={({ field: f }) => (
                                    <FormItem>
                                        <FormLabel className="text-xs">Insumo</FormLabel>
                                        <FormControl>
                                            <IngredientCombobox
                                                items={ingredientOptions}
                                                value={f.value}
                                                onChange={(id: string) => f.onChange(id)}
                                            />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            {/* Quantity */}
                            <FormField
                                control={form.control}
                                name={`lines.${index}.quantity`}
                                render={({ field: f }) => (
                                    <FormItem>
                                        <FormLabel className="text-xs">Cantidad</FormLabel>
                                        <FormControl>
                                            <Input
                                                className="h-8 text-xs"
                                                placeholder="Ej. 0,5"
                                                {...f}
                                                onBlur={(e) => {
                                                    f.onChange(formatPriceDisplay(e.target.value));
                                                }}
                                            />
                                        </FormControl>
                                    </FormItem>
                                )}
                            />

                            {/* Unit — filtered to match ingredient base unit type */}
                            <FormField
                                control={form.control}
                                name={`lines.${index}.unitId`}
                                render={({ field: f }) => {
                                    const expectedType = getUnitTypeForLine(index);
                                    const availableUnits = expectedType
                                        ? units.filter(u => u.type === expectedType)
                                        : units;

                                    return (
                                        <FormItem>
                                            <FormLabel className="text-xs">Unidad</FormLabel>
                                            <Select onValueChange={f.onChange} defaultValue={f.value} value={f.value}>
                                                <FormControl>
                                                    <SelectTrigger className="h-8 text-xs">
                                                        <SelectValue placeholder="Unidad..." />
                                                    </SelectTrigger>
                                                </FormControl>
                                                <SelectContent>
                                                    {availableUnits.map((u) => (
                                                        <SelectItem key={u.id} value={u.id}>
                                                            {u.abbreviation}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        </FormItem>
                                    );
                                }}
                            />

                            {/* Remove */}
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                className="h-8 w-8 self-end text-destructive hover:text-destructive hover:bg-destructive/10"
                                onClick={() => remove(index)}
                            >
                                <Trash2 className="h-4 w-4" />
                            </Button>
                        </div>
                    ))}
                </div>

                <div className="flex justify-end pt-2">
                    <Button type="submit" disabled={isPending || !form.formState.isValid}>
                        {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                        {isPending
                            ? "Guardando..."
                            : initialData
                                ? "Guardar cambios"
                                : "Crear preparación"}
                    </Button>
                </div>
            </form>
        </Form>
    );
}
