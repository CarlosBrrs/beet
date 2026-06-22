"use client"

import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Button } from "@/components/ui/button"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { useUnits } from "@/lib/hooks/use-units"
import { IngredientDetailResponse } from "@/lib/api-types"
import { UpdateIngredientRequest } from "@/lib/hooks/use-ingredients"
import { Loader2 } from "lucide-react"

const formSchema = z.object({
    name: z.string().trim().min(2, "Ingredient name must be at least 2 characters."),
    baseUnitId: z.string().trim().min(1, "Please select a base unit."),
})

type FormValues = z.infer<typeof formSchema>

interface IngredientEditFormProps {
    ingredient: IngredientDetailResponse
    onSubmit: (values: UpdateIngredientRequest) => void
    isSubmitting?: boolean
}

export function IngredientEditForm({ ingredient, onSubmit, isSubmitting }: IngredientEditFormProps) {
    const { data: units = [], isLoading } = useUnits()
    const form = useForm<FormValues>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            name: ingredient.name,
            baseUnitId: ingredient.baseUnitId,
        },
    })

    useEffect(() => {
        form.reset({ name: ingredient.name, baseUnitId: ingredient.baseUnitId })
    }, [form, ingredient])

    const baseUnits = units.filter((unit) => unit.isBase)

    if (isLoading) {
        return (
            <div className="flex items-center justify-center py-8 text-muted-foreground">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Loading form data...
            </div>
        )
    }

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
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

                <FormField
                    control={form.control}
                    name="baseUnitId"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Base Unit</FormLabel>
                            <Select onValueChange={field.onChange} value={field.value}>
                                <FormControl>
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select base unit" />
                                    </SelectTrigger>
                                </FormControl>
                                <SelectContent>
                                    {baseUnits.map((unit) => (
                                        <SelectItem key={unit.id} value={unit.id}>
                                            {unit.name} ({unit.abbreviation})
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <p className="text-xs text-muted-foreground">
                                Changing the base unit is only allowed before the ingredient has stock, recipes, supplier items or order history.
                            </p>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div className="flex justify-end">
                    <Button type="submit" disabled={isSubmitting || !form.formState.isValid}>
                        {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                        Save changes
                    </Button>
                </div>
            </form>
        </Form>
    )
}
