"use client"

import * as React from "react"
import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
} from "@/components/ui/sheet"

interface SheetShellProps {
    title: string
    description?: string
    children: React.ReactNode
    open: boolean
    onOpenChange: (open: boolean) => void
    size?: "default" | "sm" | "lg" | "xl" | "full" | "content"
}

export function SheetShell({
    title,
    description,
    children,
    open,
    onOpenChange,
    size = "default" // "default" in shadcn is usually "sm" (max-w-sm) or "md". Adjust based on sheet.tsx config.
}: SheetShellProps) {
    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent
                side="right"
                className="w-full sm:max-w-md flex flex-col h-full bg-background p-0 border-l data-[state=closed]:slide-out-to-right data-[state=open]:slide-in-from-right sm:max-w-md"
            >
                <div className="flex flex-col h-full">
                    <SheetHeader className="px-6 py-4 border-b">
                        <SheetTitle>{title}</SheetTitle>
                        {description && (
                            <SheetDescription>
                                {description}
                            </SheetDescription>
                        )}
                    </SheetHeader>
                    {/* 
                        Scrollable content area. 
                        We use flex-1 to take available space.
                        Padding is inside to allow scrollbar to be at the edge if needed, 
                        or we can pad the container. 
                    */}
                    <div className="flex-1 overflow-y-auto px-6 py-4">
                        {children}
                    </div>
                </div>
            </SheetContent>
        </Sheet>
    )
}
