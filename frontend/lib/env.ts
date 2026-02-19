import { z } from "zod"

const envSchema = z.object({
    NEXT_PUBLIC_API_URL: z.string().url(),
})

// Process.env is available at build time for client vars
export const env = envSchema.parse({
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
})
