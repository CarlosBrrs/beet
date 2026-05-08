"use client"

import {
    Sheet,
    SheetContent,
    SheetHeader,
    SheetTitle,
    SheetDescription,
} from "@/components/ui/sheet";
import { ProductForm } from "./product-form";

interface ProductSheetProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

export function ProductSheet({ open, onOpenChange }: ProductSheetProps) {
    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent className="sm:max-w-xl overflow-y-auto">
                <SheetHeader className="mb-6">
                    <SheetTitle>Nuevo Producto</SheetTitle>
                    <SheetDescription>
                        Crea un producto vendible y asígnalo a un submenú.
                    </SheetDescription>
                </SheetHeader>
                <ProductForm onSuccess={() => onOpenChange(false)} />
            </SheetContent>
        </Sheet>
    );
}
