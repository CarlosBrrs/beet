"use client"

import { useState } from "react";
import { SubmenuResponse } from "@/lib/api-types";
import { SubmenuForm } from "./submenu-form";
import { Button } from "@/components/ui/button";
import { Plus, Edit2, PackageOpen } from "lucide-react";

function MockProducts({ submenuName }: { submenuName: string }) {
    // Fake products just for visualizing the UI
    const MOCK_PRODUCTS = [
        { id: '1', name: `Product 1 (${submenuName})`, price: 15.99, template: "Standard Recipe" },
        { id: '2', name: `Product 2 (${submenuName})`, price: 4.99, template: "Quick Item" },
    ];

    return (
        <div className="mt-4 pt-4 border-t border-dashed">
            <div className="flex justify-between items-center mb-3">
                <h4 className="text-sm font-semibold flex items-center text-muted-foreground">
                    <PackageOpen className="w-4 h-4 mr-2" />
                    Assigned Products (Mock)
                </h4>
                <Button variant="outline" size="sm" className="h-7 text-xs">Add Product</Button>
            </div>
            <div className="space-y-2">
                {MOCK_PRODUCTS.map(p => (
                    <div key={p.id} className="flex justify-between items-center text-sm p-2 bg-muted/40 rounded">
                        <div>
                            <p className="font-medium text-foreground">{p.name}</p>
                            <p className="text-xs text-muted-foreground">Template: {p.template}</p>
                        </div>
                        <p className="font-mono">${p.price.toFixed(2)}</p>
                    </div>
                ))}
            </div>
        </div>
    )
}

interface SubmenuListProps {
    menuId: string;
    submenus: SubmenuResponse[];
}

export function SubmenuList({ menuId, submenus }: SubmenuListProps) {
    const [editingSubmenu, setEditingSubmenu] = useState<SubmenuResponse | null>(null);
    const [isCreating, setIsCreating] = useState(false);

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
                                <Button variant="ghost" size="icon" onClick={() => setEditingSubmenu(submenu)}>
                                    <Edit2 className="h-4 w-4" />
                                </Button>
                            </div>
                            <MockProducts submenuName={submenu.name} />
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
