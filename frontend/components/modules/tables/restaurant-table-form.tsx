"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2 } from "lucide-react"
import { useEffect } from "react"
import { useForm } from "react-hook-form"

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
import { Textarea } from "@/components/ui/textarea"
import { RestaurantTableResponse } from "@/lib/api-types"
import {
    useCreateRestaurantTable,
    useUpdateRestaurantTable,
} from "@/lib/hooks/use-restaurant-tables"

import { restaurantTableSchema, RestaurantTableFormValues } from "./schema"

interface RestaurantTableFormProps {
    initialData?: RestaurantTableResponse | null
    onSuccess: () => void
}

export function RestaurantTableForm({ initialData, onSuccess }: RestaurantTableFormProps) {
    const createTable = useCreateRestaurantTable()
    const updateTable = useUpdateRestaurantTable()
    const form = useForm<RestaurantTableFormValues>({
        resolver: zodResolver(restaurantTableSchema),
        defaultValues: formValues(initialData),
    })

    useEffect(() => {
        form.reset(formValues(initialData))
    }, [form, initialData])

    const isPending = createTable.isPending || updateTable.isPending

    const onSubmit = (values: RestaurantTableFormValues) => {
        const request = {
            name: values.name.trim(),
            capacity: Number(values.capacity),
            area: values.area.trim(),
            sortOrder: values.sortOrder.trim() ? Number(values.sortOrder) : 0,
            notes: values.notes.trim(),
        }

        if (initialData) {
            updateTable.mutate({ id: initialData.id, request }, { onSuccess })
            return
        }

        createTable.mutate(request, { onSuccess })
    }

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                    control={form.control}
                    name="name"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Name</FormLabel>
                            <FormControl>
                                <Input placeholder="Table 01" {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />
                <div className="grid gap-4 sm:grid-cols-2">
                    <FormField
                        control={form.control}
                        name="capacity"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Capacity</FormLabel>
                                <FormControl>
                                    <Input type="number" min="1" step="1" {...field} />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                    <FormField
                        control={form.control}
                        name="sortOrder"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Display order</FormLabel>
                                <FormControl>
                                    <Input type="number" min="0" step="1" placeholder="0" {...field} />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                </div>
                <FormField
                    control={form.control}
                    name="area"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Area</FormLabel>
                            <FormControl>
                                <Input placeholder="Terrace" {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />
                <FormField
                    control={form.control}
                    name="notes"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Notes</FormLabel>
                            <FormControl>
                                <Textarea placeholder="Optional notes" className="resize-none" {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />
                <div className="flex justify-end pt-2">
                    <Button type="submit" disabled={isPending}>
                        {isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                        {initialData ? "Save changes" : "Create table"}
                    </Button>
                </div>
            </form>
        </Form>
    )
}

function formValues(table?: RestaurantTableResponse | null): RestaurantTableFormValues {
    return {
        name: table?.name ?? "",
        capacity: table ? String(table.capacity) : "1",
        area: table?.area ?? "",
        sortOrder: table ? String(table.sortOrder) : "0",
        notes: table?.notes ?? "",
    }
}
