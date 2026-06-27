import { z } from "zod"

export const cashRegisterSchema = z.object({
    name: z.string().trim().min(1, "Name is required").max(120, "Name must have at most 120 characters"),
    notes: z.string(),
})

export type CashRegisterFormValues = z.infer<typeof cashRegisterSchema>
