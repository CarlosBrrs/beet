"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2, Save } from "lucide-react"
import { useEffect, useMemo } from "react"
import { useForm, useWatch } from "react-hook-form"
import { z } from "zod"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import { useMyPermissions } from "@/lib/hooks/use-my-permissions"
import { useRestaurant, useUpdateRestaurant } from "@/lib/hooks/use-restaurants"

const timeZones = [
    "America/Bogota",
    "America/Lima",
    "America/Mexico_City",
    "America/Santiago",
    "America/New_York",
]

const settingsSchema = z.object({
    name: z.string().trim().min(3, "Name must be at least 3 characters"),
    operationMode: z.enum(["PREPAID", "POSTPAID"]),
    isActive: z.boolean(),
    phoneNumber: z.string().trim(),
    address: z.string().trim(),
    email: z.email("Invalid email").or(z.literal("")),
    settings: z.object({
        prePaymentEnabled: z.boolean(),
        allowTakeaway: z.boolean(),
        allowDelivery: z.boolean(),
        maxTableCapacity: z.coerce.number<number>().int().min(1, "Capacity must be at least 1"),
        taxApplyMode: z.enum(["PER_INVOICE", "PER_ITEM"]),
        defaultTaxPercentage: z.coerce.number<number>().min(0, "Tax cannot be negative"),
        timeZone: z.string().trim().min(1, "Time zone is required"),
        prepaidOrderExpirationMinutes: z.coerce.number<number>().int().min(5).max(1440),
        cashCountMode: z.enum(["BLIND", "VISIBLE"]),
    }),
})

type SettingsValues = z.infer<typeof settingsSchema>

const defaultValues: SettingsValues = {
    name: "",
    operationMode: "PREPAID",
    isActive: true,
    phoneNumber: "",
    address: "",
    email: "",
    settings: {
        prePaymentEnabled: false,
        allowTakeaway: true,
        allowDelivery: true,
        maxTableCapacity: 1,
        taxApplyMode: "PER_INVOICE",
        defaultTaxPercentage: 0,
        timeZone: "America/Bogota",
        prepaidOrderExpirationMinutes: 30,
        cashCountMode: "BLIND",
    },
}

export default function RestaurantSettingsPage() {
    const { restaurantId } = useRestaurantContext()
    const { can, isLoading: permissionsLoading } = useMyPermissions()
    const restaurantQuery = useRestaurant(restaurantId)
    const updateRestaurant = useUpdateRestaurant(restaurantId)
    const canEdit = can("EDIT", "RESTAURANTS", restaurantId)

    const form = useForm<SettingsValues>({
        resolver: zodResolver(settingsSchema),
        mode: "onChange",
        defaultValues,
    })

    const baselineValues = useMemo<SettingsValues>(() => {
        const restaurant = restaurantQuery.data
        if (!restaurant) return defaultValues

        return {
            name: restaurant.name ?? "",
            operationMode: restaurant.operationMode ?? "PREPAID",
            isActive: restaurant.isActive ?? true,
            phoneNumber: restaurant.phoneNumber ?? "",
            address: restaurant.address ?? "",
            email: restaurant.email ?? "",
            settings: {
                prePaymentEnabled: restaurant.settings?.prePaymentEnabled ?? false,
                allowTakeaway: restaurant.settings?.allowTakeaway ?? true,
                allowDelivery: restaurant.settings?.allowDelivery ?? true,
                maxTableCapacity: restaurant.settings?.maxTableCapacity ?? 1,
                taxApplyMode: restaurant.settings?.taxApplyMode ?? "PER_INVOICE",
                defaultTaxPercentage: restaurant.settings?.defaultTaxPercentage ?? 0,
                timeZone: restaurant.settings?.timeZone ?? "America/Bogota",
                prepaidOrderExpirationMinutes: restaurant.settings?.prepaidOrderExpirationMinutes ?? 30,
                cashCountMode: restaurant.settings?.cashCountMode ?? "BLIND",
            },
        }
    }, [restaurantQuery.data])
    useEffect(() => {
        if (!restaurantQuery.data) return
        form.reset(baselineValues)
    }, [baselineValues, form, restaurantQuery.data])

    const watchedValues = useWatch({ control: form.control })
    const hasChanges = useMemo(() => (
        JSON.stringify(watchedValues) !== JSON.stringify(baselineValues)
    ), [baselineValues, watchedValues])

    const disabled = !canEdit || updateRestaurant.isPending

    if (restaurantQuery.isLoading || permissionsLoading) {
        return (
            <div className="flex min-h-[320px] items-center justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
        )
    }

    if (restaurantQuery.isError) {
        return (
            <div className="border border-destructive/40 bg-destructive/5 p-4 text-sm text-destructive">
                No se pudo cargar la configuracion del restaurante.
            </div>
        )
    }

    return (
        <div className="space-y-6 pb-14">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Restaurant Settings</h1>
                    <p className="mt-1 text-sm text-muted-foreground">
                        Configura la operacion, caja, mesas, impuestos y datos basicos del restaurante.
                    </p>
                </div>
                {!canEdit && (
                    <div className="border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
                        Solo lectura. Necesitas RESTAURANTS:EDIT para modificar esta configuracion.
                    </div>
                )}
            </div>

            <Form {...form}>
                <form
                    className="space-y-6"
                    onSubmit={form.handleSubmit((values) => updateRestaurant.mutate(values))}
                >
                    <Card>
                        <CardHeader>
                            <CardTitle>General</CardTitle>
                            <CardDescription>Datos visibles y estado administrativo del restaurante.</CardDescription>
                        </CardHeader>
                        <CardContent className="grid gap-4 md:grid-cols-2">
                            <FormField control={form.control} name="name" render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>
                                    <FormControl><Input disabled={disabled} {...field} /></FormControl>
                                    <FormMessage />
                                </FormItem>
                            )} />
                            <FormField control={form.control} name="phoneNumber" render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Phone</FormLabel>
                                    <FormControl><Input disabled={disabled} {...field} /></FormControl>
                                    <FormMessage />
                                </FormItem>
                            )} />
                            <FormField control={form.control} name="email" render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Email</FormLabel>
                                    <FormControl><Input disabled={disabled} {...field} /></FormControl>
                                    <FormMessage />
                                </FormItem>
                            )} />
                            <FormField control={form.control} name="isActive" render={({ field }) => (
                                <FormItem className="flex items-center justify-between border px-3 py-2">
                                    <div>
                                        <FormLabel>Active</FormLabel>
                                        <FormDescription>Desactiva el restaurante sin borrar historicos.</FormDescription>
                                    </div>
                                    <FormControl><Switch disabled={disabled} checked={field.value} onCheckedChange={field.onChange} /></FormControl>
                                </FormItem>
                            )} />
                            <FormField control={form.control} name="address" render={({ field }) => (
                                <FormItem className="md:col-span-2">
                                    <FormLabel>Address</FormLabel>
                                    <FormControl><Textarea disabled={disabled} {...field} /></FormControl>
                                    <FormMessage />
                                </FormItem>
                            )} />
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle>Operacion</CardTitle>
                            <CardDescription>Define el flujo comercial y los tipos de servicio habilitados.</CardDescription>
                        </CardHeader>
                        <CardContent className="grid gap-4 md:grid-cols-3">
                            <FormField control={form.control} name="operationMode" render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Operation Mode</FormLabel>
                                    <Select disabled={disabled} value={field.value} onValueChange={field.onChange}>
                                        <FormControl><SelectTrigger className="w-full"><SelectValue /></SelectTrigger></FormControl>
                                        <SelectContent>
                                            <SelectItem value="PREPAID">Prepaid</SelectItem>
                                            <SelectItem value="POSTPAID">Postpaid</SelectItem>
                                        </SelectContent>
                                    </Select>
                                    <FormDescription>PREPAID exige pago antes de enviar a cocina.</FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )} />
                            <BooleanField disabled={disabled} form={form} name="settings.allowTakeaway" label="Takeout" />
                            <BooleanField disabled={disabled} form={form} name="settings.allowDelivery" label="Delivery" />
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle>Ordenes Prepago</CardTitle>
                            <CardDescription>Controla cuanto tiempo se conservan reservas pendientes de pago.</CardDescription>
                        </CardHeader>
                        <CardContent className="grid gap-4 md:grid-cols-2">
                            <FormField control={form.control} name="settings.prepaidOrderExpirationMinutes" render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Expiration minutes</FormLabel>
                                    <FormControl><Input type="number" min={5} max={1440} disabled={disabled} {...field} /></FormControl>
                                    <FormDescription>Rango permitido: 5 a 1440 minutos.</FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )} />
                            <BooleanField disabled={disabled} form={form} name="settings.prePaymentEnabled" label="Prepayment flag" description="Compatibilidad interna; el flujo real usa Operation Mode." />
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle>Mesas y Caja</CardTitle>
                            <CardDescription>Parametros usados por mesas, arqueos y cierres.</CardDescription>
                        </CardHeader>
                        <CardContent className="grid gap-4 md:grid-cols-2">
                            <FormField control={form.control} name="settings.maxTableCapacity" render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Max table capacity</FormLabel>
                                    <FormControl><Input type="number" min={1} disabled={disabled} {...field} /></FormControl>
                                    <FormMessage />
                                </FormItem>
                            )} />
                            <FormField control={form.control} name="settings.cashCountMode" render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Cash count mode</FormLabel>
                                    <Select disabled={disabled} value={field.value} onValueChange={field.onChange}>
                                        <FormControl><SelectTrigger className="w-full"><SelectValue /></SelectTrigger></FormControl>
                                        <SelectContent>
                                            <SelectItem value="BLIND">Blind</SelectItem>
                                            <SelectItem value="VISIBLE">Visible</SelectItem>
                                        </SelectContent>
                                    </Select>
                                    <FormDescription>Blind oculta el esperado antes del conteo.</FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )} />
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle>Zona Horaria</CardTitle>
                            <CardDescription>Afecta numeracion diaria, cajas, cortes y reportes futuros.</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <FormField control={form.control} name="settings.timeZone" render={({ field }) => (
                                <FormItem className="max-w-sm">
                                    <FormLabel>Time zone</FormLabel>
                                    <Select disabled={disabled} value={field.value} onValueChange={field.onChange}>
                                        <FormControl><SelectTrigger className="w-full"><SelectValue /></SelectTrigger></FormControl>
                                        <SelectContent>
                                            {timeZones.map((timeZone) => <SelectItem key={timeZone} value={timeZone}>{timeZone}</SelectItem>)}
                                        </SelectContent>
                                    </Select>
                                    <FormDescription>Los historicos no se recalculan al cambiarla.</FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )} />
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle>Impuestos</CardTitle>
                            <CardDescription>Los precios actuales se manejan como brutos; estos campos preparan reportes y reglas futuras.</CardDescription>
                        </CardHeader>
                        <CardContent className="grid gap-4 md:grid-cols-2">
                            <FormField control={form.control} name="settings.taxApplyMode" render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Tax apply mode</FormLabel>
                                    <Select disabled={disabled} value={field.value} onValueChange={field.onChange}>
                                        <FormControl><SelectTrigger className="w-full"><SelectValue /></SelectTrigger></FormControl>
                                        <SelectContent>
                                            <SelectItem value="PER_INVOICE">Per invoice</SelectItem>
                                            <SelectItem value="PER_ITEM">Per item</SelectItem>
                                        </SelectContent>
                                    </Select>
                                    <FormMessage />
                                </FormItem>
                            )} />
                            <FormField control={form.control} name="settings.defaultTaxPercentage" render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Default tax percentage</FormLabel>
                                    <FormControl><Input type="number" min={0} step="0.01" disabled={disabled} {...field} /></FormControl>
                                    <FormMessage />
                                </FormItem>
                            )} />
                        </CardContent>
                    </Card>

                    {canEdit && (
                        <div className="sticky bottom-3 flex justify-end border bg-background/95 p-3 backdrop-blur">
                            <Button type="submit" disabled={!hasChanges || updateRestaurant.isPending}>
                                {updateRestaurant.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                                Guardar cambios
                            </Button>
                        </div>
                    )}
                </form>
            </Form>
        </div>
    )
}

function BooleanField({
    disabled,
    form,
    name,
    label,
    description,
}: {
    disabled: boolean
    form: ReturnType<typeof useForm<SettingsValues>>
    name: "settings.allowTakeaway" | "settings.allowDelivery" | "settings.prePaymentEnabled"
    label: string
    description?: string
}) {
    return (
        <FormField control={form.control} name={name} render={({ field }) => (
            <FormItem className="flex items-center justify-between border px-3 py-2">
                <div>
                    <FormLabel>{label}</FormLabel>
                    {description && <FormDescription>{description}</FormDescription>}
                </div>
                <FormControl><Switch disabled={disabled} checked={field.value} onCheckedChange={field.onChange} /></FormControl>
            </FormItem>
        )} />
    )
}
