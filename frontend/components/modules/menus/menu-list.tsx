"use client"

import { useState } from "react";
import Link from "next/link";
import { useMenus } from "@/lib/hooks/use-menus";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Edit2, Loader2, ListTree, ChevronDown, ArrowRight } from "lucide-react";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import { cn } from "@/lib/utils";
import { MenuResponse } from "@/lib/api-types";

interface MenuListProps {
    onEditMenu: (menuId: string) => void;
}

export function MenuList({ onEditMenu }: MenuListProps) {
    const { data: menus, isLoading, isError } = useMenus();

    if (isLoading) {
        return (
            <div className="flex py-12 items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
        );
    }

    if (isError) {
        return (
            <div className="text-center py-12 text-destructive">
                Failed to load menus. Please try again later.
            </div>
        );
    }

    if (!menus || menus.length === 0) {
        return (
            <div className="text-center py-12 border border-dashed rounded-lg bg-muted/20">
                <p className="text-muted-foreground">No menus found.</p>
            </div>
        );
    }

    return (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 items-start">
            {menus.map((menu) => (
                <MenuCard key={menu.id} menu={menu} onEditMenu={onEditMenu} />
            ))}
        </div>
    );
}

function MenuCard({ menu, onEditMenu }: { menu: MenuResponse, onEditMenu: (id: string) => void }) {
    const [isOpen, setIsOpen] = useState(false);

    // Filter and sort submenus
    const submenus = [...(menu.submenus || [])].sort((a, b) => a.sortOrder - b.sortOrder);

    return (
        <Card className="relative overflow-hidden group flex flex-col h-full bg-card">
            <CardHeader className="pb-3 flex-none">
                <CardTitle className="flex justify-between items-start gap-2">
                    <span className="truncate leading-tight mt-1">{menu.name}</span>
                    <Button
                        variant="secondary"
                        size="icon"
                        className="h-8 w-8 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity"
                        onClick={() => onEditMenu(menu.id)}
                    >
                        <Edit2 className="h-4 w-4" />
                    </Button>
                </CardTitle>
                {menu.description && (
                    <CardDescription className="line-clamp-2 text-xs">
                        {menu.description}
                    </CardDescription>
                )}
            </CardHeader>
            <CardContent className="flex-1 flex flex-col">
                <Collapsible open={isOpen} onOpenChange={setIsOpen} className="w-full">
                    <div className="flex items-center justify-between pb-2">
                        <div className="flex items-center text-sm text-muted-foreground mr-2 font-medium">
                            <ListTree className="mr-2 h-4 w-4 text-primary" />
                            {submenus.length} submenus
                        </div>
                        {submenus.length > 0 && (
                            <CollapsibleTrigger asChild>
                                <Button variant="ghost" size="sm" className="h-7 text-xs px-2 hover:bg-muted/60">
                                    {isOpen ? "Hide" : "View"}
                                    <ChevronDown className={cn("ml-1.5 h-3.5 w-3.5 transition-transform duration-200", isOpen && "rotate-180")} />
                                </Button>
                            </CollapsibleTrigger>
                        )}
                    </div>
                    <CollapsibleContent className="space-y-3 mt-3 pt-3 border-t">
                        {submenus.map(sub => (
                            <div key={sub.id} className="border rounded-md p-3 bg-muted/10 shadow-sm">
                                <h5 className="font-semibold text-sm mb-2 text-foreground tracking-tight">{sub.name}</h5>
                                <Button asChild variant="outline" size="sm" className="h-8 w-full justify-between text-xs">
                                    <Link href={`/restaurants/${menu.restaurantId}/menus/${menu.id}/submenus/${sub.id}`}>
                                        Gestionar elementos
                                        <ArrowRight className="h-3.5 w-3.5" />
                                    </Link>
                                </Button>
                            </div>
                        ))}
                    </CollapsibleContent>
                </Collapsible>
            </CardContent>
        </Card>
    );
}
