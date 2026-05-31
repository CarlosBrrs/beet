import { z } from "zod"

const amountSchema = z.string()
    .trim()
    .min(1, "Amount is required")
    .refine((value) => /^\d{1,3}(\.\d{3})*(,\d{1,2})?$|^\d+(,\d{1,2})?$/.test(value), "Enter a valid amount")

export const openCashSessionSchema = z.object({
    cashRegisterId: z.string().min(1, "Select a cash register"),
    openingAmount: amountSchema,
    notes: z.string(),
})

export const closeCashSessionSchema = z.object({
    closingAmount: amountSchema,
    notes: z.string(),
})

export type OpenCashSessionFormValues = z.infer<typeof openCashSessionSchema>
export type CloseCashSessionFormValues = z.infer<typeof closeCashSessionSchema>
