"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2 } from "lucide-react"
import Link from "next/link"
import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"

import { DialogShell } from "@/components/shared/dialog-shell"
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { RestaurantResponse } from "@/lib/api-types"
import { useUpdateRestaurant } from "@/lib/hooks/use-restaurants"

const schema = z.object({
    expirationMinutes: z.coerce.number<number>().int().min(5).max(1440),
})

interface PrepaidSettingsDialogProps {
    restaurant: RestaurantResponse
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function PrepaidSettingsDialog({ restaurant, open, onOpenChange }: PrepaidSettingsDialogProps) {
    const update = useUpdateRestaurant(restaurant.id)
    const form = useForm<z.infer<typeof schema>>({
        resolver: zodResolver(schema),
        defaultValues: { expirationMinutes: restaurant.settings.prepaidOrderExpirationMinutes ?? 30 },
    })

    useEffect(() => {
        if (open) {
            form.reset({ expirationMinutes: restaurant.settings.prepaidOrderExpirationMinutes ?? 30 })
        }
    }, [form, open, restaurant.settings.prepaidOrderExpirationMinutes])

    return (
        <DialogShell
            open={open}
            onOpenChange={onOpenChange}
            title="Configuracion prepago"
            description="Define cuanto tiempo se conserva una reserva mientras la orden espera el pago."
        >
            <Form {...form}>
                <form
                    onSubmit={form.handleSubmit((values) => update.mutate({
                        name: restaurant.name,
                        operationMode: restaurant.operationMode,
                        isActive: restaurant.isActive,
                        settings: {
                            ...restaurant.settings,
                            prepaidOrderExpirationMinutes: values.expirationMinutes,
                        },
                    }, { onSuccess: () => onOpenChange(false) }))}
                    className="space-y-5"
                >
                    <FormField
                        control={form.control}
                        name="expirationMinutes"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Vencimiento en minutos</FormLabel>
                                <FormControl>
                                    <Input type="number" min={5} max={1440} {...field} />
                                </FormControl>
                                <FormDescription>
                                    Las ordenes sin pago se cancelan. Las parcialmente pagadas liberan stock y requieren reactivacion.
                                </FormDescription>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                    <div className="flex items-center justify-between gap-2">
                        <Button variant="link" className="px-0" asChild>
                            <Link href={`/restaurants/${restaurant.id}/settings`}>Abrir configuracion completa</Link>
                        </Button>
                        <div className="flex gap-2">
                            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancelar</Button>
                            <Button type="submit" disabled={update.isPending}>
                                {update.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                                Guardar
                            </Button>
                        </div>
                    </div>
                </form>
            </Form>
        </DialogShell>
    )
}
