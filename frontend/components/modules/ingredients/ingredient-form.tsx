"use client"

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
import { CreateIngredientRequest, Ingredient } from "@/lib/api-types"
import { useEffect } from "react"

const formSchema = z.object({
    name: z.string().min(2, { message: "Name must be at least 2 characters." }),
    unit: z.string().min(1, { message: "Please select a unit." }),
    cost: z.number().min(0, { message: "Cost must be positive." }),
})

interface IngredientFormProps {
    defaultValues?: Ingredient
    onSubmit: (values: CreateIngredientRequest) => void
    isSubmitting?: boolean
}

export function IngredientForm({ defaultValues, onSubmit, isSubmitting }: IngredientFormProps) {
    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            name: "",
            unit: "",
            cost: 0,
        },
    })

    // Reset form when defaultValues change (e.g. opening edit mode)
    useEffect(() => {
        if (defaultValues) {
            form.reset({
                name: defaultValues.name,
                unit: defaultValues.unit,
                cost: defaultValues.cost,
            })
        } else {
            form.reset({ name: "", unit: "", cost: 0 })
        }
    }, [defaultValues, form])

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
                                <Input placeholder="e.g. Tomato" {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div className="grid grid-cols-2 gap-4">
                    <FormField
                        control={form.control}
                        name="unit"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Unit</FormLabel>
                                <Select
                                    onValueChange={field.onChange}
                                    value={field.value}
                                >
                                    <FormControl>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Select unit" />
                                        </SelectTrigger>
                                    </FormControl>
                                    <SelectContent>
                                        <SelectItem value="kg">Kilogram (kg)</SelectItem>
                                        <SelectItem value="g">Gram (g)</SelectItem>
                                        <SelectItem value="l">Liter (l)</SelectItem>
                                        <SelectItem value="ml">Milliliter (ml)</SelectItem>
                                        <SelectItem value="unit">Unit (pcs)</SelectItem>
                                    </SelectContent>
                                </Select>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <FormField
                        control={form.control}
                        name="cost"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Cost</FormLabel>
                                <FormControl>
                                    <Input type="number" step="0.01" placeholder="0.00" {...field} />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                </div>

                <div className="flex justify-end pt-4">
                    <Button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? "Saving..." : defaultValues ? "Update Ingredient" : "Create Ingredient"}
                    </Button>
                </div>
            </form>
        </Form>
    )
}
