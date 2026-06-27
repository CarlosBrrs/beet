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
import { CashRegisterResponse } from "@/lib/api-types"
import { useCreateCashRegister, useUpdateCashRegister } from "@/lib/hooks/use-cash-registers"

import { cashRegisterSchema, CashRegisterFormValues } from "./schema"

interface CashRegisterFormProps {
    initialData?: CashRegisterResponse | null
    onSuccess: () => void
}

export function CashRegisterForm({ initialData, onSuccess }: CashRegisterFormProps) {
    const createRegister = useCreateCashRegister()
    const updateRegister = useUpdateCashRegister()
    const form = useForm<CashRegisterFormValues>({
        resolver: zodResolver(cashRegisterSchema),
        defaultValues: {
            name: initialData?.name ?? "",
            notes: initialData?.notes ?? "",
        },
    })

    useEffect(() => {
        form.reset({
            name: initialData?.name ?? "",
            notes: initialData?.notes ?? "",
        })
    }, [form, initialData])

    const isPending = createRegister.isPending || updateRegister.isPending

    const onSubmit = (values: CashRegisterFormValues) => {
        const request = {
            name: values.name.trim(),
            notes: values.notes.trim(),
        }

        if (initialData) {
            updateRegister.mutate(
                { id: initialData.id, request },
                { onSuccess }
            )
            return
        }

        createRegister.mutate(request, { onSuccess })
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
                                <Input placeholder="Main counter" {...field} />
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
                        {initialData ? "Save changes" : "Create cash register"}
                    </Button>
                </div>
            </form>
        </Form>
    )
}
