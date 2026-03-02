import { z } from "zod";

export const menuSchema = z.object({
    name: z.string().trim().min(2, "Name must be at least 2 characters").max(255, "Name is too long"),
    description: z.string().trim().optional(),
});

export type MenuFormValues = z.infer<typeof menuSchema>;

export const submenuSchema = z.object({
    name: z.string().trim().min(2, "Name must be at least 2 characters").max(255, "Name is too long"),
    description: z.string().trim().optional(),
    sortOrder: z.number().int().min(0, "Must be positive").optional(),
});

export type SubmenuFormValues = z.infer<typeof submenuSchema>;
