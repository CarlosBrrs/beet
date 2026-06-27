"use client"

import { useMenus } from "@/lib/hooks/use-menus";
import { SheetShell } from "@/components/shared/sheet-shell";
import { MenuForm } from "./menu-form";
import { SubmenuList } from "./submenu-list";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Can } from "@/components/shared/can";
import { Button } from "@/components/ui/button";

interface MenuDetailSheetProps {
    menuId: string | null;
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

export function MenuDetailSheet({ menuId, open, onOpenChange }: MenuDetailSheetProps) {
    const { data: menus } = useMenus();

    // If we have a menuId, find it; if open but no menuId, it's creation mode
    const menuInfo = menus?.find((m) => m.id === menuId);

    const title = menuInfo ? `Edit Menu: ${menuInfo.name}` : "Create Menu";
    const desc = menuInfo ? "Update menu details and manage submenus." : "Set up a new menu.";

    return (
        <SheetShell
            open={open}
            onOpenChange={onOpenChange}
            title={title}
            description={desc}
            size="xl"
        >
            <div className="mt-6 flex flex-col gap-6">
                {!menuInfo ? (
                    // Creation Mode
                    <Can I="CREATE" a="MENUS">
                        <MenuForm onSuccess={() => onOpenChange(false)} />
                    </Can>
                ) : (
                    // Edit Mode
                    <Tabs defaultValue="details" className="w-full">
                        <TabsList className="grid w-full grid-cols-2">
                            <TabsTrigger value="details">Details</TabsTrigger>
                            <TabsTrigger value="submenus">Submenus</TabsTrigger>
                        </TabsList>
                        <TabsContent value="details" className="mt-4">
                            <Can I="EDIT" a="MENUS">
                                <MenuForm initialData={menuInfo} onSuccess={() => onOpenChange(false)} />
                            </Can>
                        </TabsContent>
                        <TabsContent value="submenus" className="mt-4">
                            <Can I="VIEW" a="MENUS">
                                <SubmenuList menuId={menuInfo.id} submenus={menuInfo.submenus} />
                            </Can>
                        </TabsContent>
                    </Tabs>
                )}
            </div>
        </SheetShell>
    );
}
