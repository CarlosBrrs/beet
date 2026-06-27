"use client"

import { useState } from "react";
import { SubmenuResponse } from "@/lib/api-types";
import { SubmenuForm } from "./submenu-form";
import { Button } from "@/components/ui/button";
import { Plus, Edit2, Eye } from "lucide-react";
import Link from "next/link";
import { useRestaurantContext } from "@/components/providers/restaurant-provider";

interface SubmenuListProps {
    menuId: string;
    submenus: SubmenuResponse[];
}

export function SubmenuList({ menuId, submenus }: SubmenuListProps) {
    const [editingSubmenu, setEditingSubmenu] = useState<SubmenuResponse | null>(null);
    const [isCreating, setIsCreating] = useState(false);
    const { restaurantId } = useRestaurantContext();

    if (isCreating) {
        return (
            <div className="space-y-4">
                <Button variant="ghost" onClick={() => setIsCreating(false)}>
                    ← Back to List
                </Button>
                <SubmenuForm menuId={menuId} onSuccess={() => setIsCreating(false)} />
            </div>
        );
    }

    if (editingSubmenu) {
        return (
            <div className="space-y-4">
                <Button variant="ghost" onClick={() => setEditingSubmenu(null)}>
                    ← Back to List
                </Button>
                <SubmenuForm
                    menuId={menuId}
                    initialData={editingSubmenu}
                    onSuccess={() => setEditingSubmenu(null)}
                />
            </div>
        );
    }

    // Sort by sortOrder
    const sortedSubmenus = [...submenus].sort((a, b) => a.sortOrder - b.sortOrder);

    return (
        <div className="space-y-4">
            <div className="flex justify-end">
                <Button size="sm" onClick={() => setIsCreating(true)}>
                    <Plus className="mr-2 h-4 w-4" />
                    Add Submenu
                </Button>
            </div>

            {sortedSubmenus.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground border border-dashed rounded-md">
                    No submenus defined yet.
                </div>
            ) : (
                <div className="space-y-2">
                    {sortedSubmenus.map((submenu) => (
                        <div key={submenu.id} className="flex flex-col p-4 border rounded-md bg-card">
                            <div className="flex items-start justify-between">
                                <div>
                                    <p className="font-semibold text-base">{submenu.name}</p>
                                    {submenu.description && (
                                        <p className="text-sm text-muted-foreground">{submenu.description}</p>
                                    )}
                                </div>
                                <div className="flex gap-2">
                                    <Button variant="secondary" size="sm" asChild>
                                        <Link href={`/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenu.id}`}>
                                            <Eye className="mr-2 h-4 w-4" /> Elementos
                                        </Link>
                                    </Button>
                                    <Button variant="ghost" size="icon" onClick={() => setEditingSubmenu(submenu)}>
                                        <Edit2 className="h-4 w-4" />
                                    </Button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
