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
import { menuSchema, MenuFormValues } from "./schema";
import { useCreateMenu, useUpdateMenu } from "@/lib/hooks/use-menus";
import { MenuResponse } from "@/lib/api-types";

interface MenuFormProps {
    initialData?: MenuResponse;
    onSuccess?: () => void;
}

export function MenuForm({ initialData, onSuccess }: MenuFormProps) {
    const createMenu = useCreateMenu();
    const updateMenu = useUpdateMenu();

    const form = useForm<MenuFormValues>({
        resolver: zodResolver(menuSchema),
        defaultValues: {
            name: initialData?.name || "",
            description: initialData?.description || "",
        },
    });

    const isPending = createMenu.isPending || updateMenu.isPending;

    const onSubmit = (data: MenuFormValues) => {
        if (initialData) {
            updateMenu.mutate(
                { id: initialData.id, data },
                { onSuccess: () => onSuccess?.() }
            );
        } else {
            createMenu.mutate(data, { onSuccess: () => onSuccess?.() });
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
                            <FormLabel>Menu Name</FormLabel>
                            <FormControl>
                                <Input placeholder="e.g. Breakfast, Lunch, Drinks..." {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />
                <FormField
                    control={form.control}
                    name="description"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Description (Optional)</FormLabel>
                            <FormControl>
                                <Textarea
                                    placeholder="Briefly describe this menu"
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
                        {isPending ? "Saving..." : initialData ? "Save Changes" : "Create Menu"}
                    </Button>
                </div>
            </form>
        </Form>
    );
}
