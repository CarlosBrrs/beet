---
name: scaffold_frontend_feature
description: Generates a new frontend feature module with List and Form components, and a proper page route.
---

# Scaffold Frontend Feature

This skill generates a vertical slice for a new domain feature in the frontend.

## Usage

User will provide: `FeatureName` (e.g., `Menu`, `Order`)

## Actions

1.  **Create Directory Structure**:
    *   `components/modules/[FeatureName]/`
    *   `app/(dashboard)/[feature-name]/`

2.  **Create List Component**:
    *   Path: `components/modules/[FeatureName]/[FeatureName]List.tsx`
    *   Content: A placeholder `Table` or `Card` list view.

3.  **Create Form Component**:
    *   Path: `components/modules/[FeatureName]/[FeatureName]Form.tsx`
    *   Content: A `react-hook-form` + `zod` form with a basic schema.

4.  **Create Page Wrapper**:
    *   Path: `app/(dashboard)/[feature-name]/page.tsx`
    *   Content: A server component (or client wrapper) that renders `[FeatureName]List`.

## Templates

### List Component
```tsx
"use client"

import { Button } from "@/components/ui/button"
import { Plus } from "lucide-react"

export function [FeatureName]List() {
    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold tracking-tight">[FeatureName]s</h2>
                <Button>
                    <Plus className="mr-2 h-4 w-4" /> Add [FeatureName]
                </Button>
            </div>
            <div className="border rounded-md p-8 text-center text-muted-foreground">
                List of [feature-name]s will go here.
            </div>
        </div>
    )
}
```

### Form Component
```tsx
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
import { Loader2 } from "lucide-react"

const formSchema = z.object({
  name: z.string().min(2, "Name must be at least 2 characters."),
})

type FormValues = z.infer<typeof formSchema>

interface [FeatureName]FormProps {
  onSubmit: (values: FormValues) => void
  isSubmitting?: boolean
}

export function [FeatureName]Form({ onSubmit, isSubmitting }: [FeatureName]FormProps) {
    const form = useForm<FormValues>({
        resolver: zodResolver(formSchema),
        mode: "onChange", // Required — makes formState.isValid reactive
        defaultValues: { name: "" },
    })

    // NOTE: If you use form.setValue() to clear/change a field that affects
    // other fields' validity, always follow it with form.trigger("affectedField").
    // See frontend_patterns.md §5.4 for details.

    // NOTE: For monetary inputs, use formatPriceDisplay / parsePriceInput
    // from @/lib/formatters instead of type="number". See frontend_patterns.md §6.

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                <FormField
                    control={form.control}
                    name="name"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Name</FormLabel>
                            <FormControl>
                                <Input placeholder="..." {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                {/* Always disable when invalid OR submitting */}
                <div className="flex justify-end">
                    <Button type="submit" disabled={isSubmitting || !form.formState.isValid}>
                        {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                        {isSubmitting ? "Saving..." : "Save"}
                    </Button>
                </div>
            </form>
        </Form>
    )
}
```

### Page Component
```tsx
import { [FeatureName]List } from "@/components/modules/[FeatureName]/[FeatureName]List"

export default function [FeatureName]Page() {
    return (
        <div className="container mx-auto py-10">
            <[FeatureName]List />
        </div>
    )
}
```
