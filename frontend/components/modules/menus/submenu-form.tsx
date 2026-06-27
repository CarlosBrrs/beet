"use client"

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { submenuSchema, SubmenuFormValues } from "./schema";
import { useCreateSubmenu, useUpdateSubmenu } from "@/lib/hooks/use-menus";
import { SubmenuResponse } from "@/lib/api-types";

interface SubmenuFormProps {
    menuId: string;
    initialData?: SubmenuResponse;
    onSuccess?: () => void;
}

export function SubmenuForm({ menuId, initialData, onSuccess }: SubmenuFormProps) {
    const createSubmenu = useCreateSubmenu();
    const updateSubmenu = useUpdateSubmenu();

    const form = useForm<SubmenuFormValues>({
        resolver: zodResolver(submenuSchema),
        defaultValues: {
            name: initialData?.name || "",
            description: initialData?.description || "",
            sortOrder: initialData?.sortOrder || 0,
        },
    });

    const isPending = createSubmenu.isPending || updateSubmenu.isPending;

    const onSubmit = (data: SubmenuFormValues) => {
        if (initialData) {
            updateSubmenu.mutate(
                { menuId, id: initialData.id, data },
                { onSuccess: () => onSuccess?.() }
            );
        } else {
            createSubmenu.mutate({ menuId, data }, { onSuccess: () => onSuccess?.() });
        }
    };

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                    control={form.control}
                    name="name"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Submenu Name</FormLabel>
                            <FormControl>
                                <Input placeholder="e.g. Starters, Main Course..." {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div className="grid grid-cols-2 gap-4">
                    <FormField
                        control={form.control}
                        name="sortOrder"
                        render={({ field }) => (
                            <FormItem className="col-span-2 sm:col-span-1">
                                <FormLabel>Sort Order</FormLabel>
                                <FormControl>
                                    <Input
                                        type="number"
                                        {...field}
                                        onChange={(e) => field.onChange(parseInt(e.target.value))}
                                    />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                </div>

                <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Description (Optional)</FormLabel>
                            <FormControl>
                                <Textarea
                                    placeholder="Briefly describe this category"
                                    className="resize-none"
                                    {...field}
                                    value={field.value || ""}
                                />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />
                <div className="flex justify-end gap-2 pt-4">
                    <Button type="submit" disabled={isPending}>
                        {isPending ? "Saving..." : initialData ? "Save Changes" : "Create Submenu"}
                    </Button>
                </div>
            </form>
        </Form>
    );
}
