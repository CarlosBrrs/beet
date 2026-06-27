import { z } from "zod"

const positiveInteger = (value: string) => {
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed > 0
}

const optionalNonNegativeInteger = (value: string) => {
    if (!value.trim()) return true
    const parsed = Number(value)
    return Number.isInteger(parsed) && parsed >= 0
}

export const restaurantTableSchema = z.object({
    name: z.string().trim().min(1, "Name is required").max(120, "Name must have at most 120 characters"),
    capacity: z.string().trim().refine(positiveInteger, "Capacity must be a positive integer"),
    area: z.string().trim().max(120, "Area must have at most 120 characters"),
    sortOrder: z.string().trim().refine(optionalNonNegativeInteger, "Order must be zero or a positive integer"),
    notes: z.string(),
})

export type RestaurantTableFormValues = z.infer<typeof restaurantTableSchema>
