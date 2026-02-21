"use client"

import * as React from "react"
import {
    Sheet,
    SheetContent,
    SheetDescription,
    SheetHeader,
    SheetTitle,
} from "@/components/ui/sheet"
import { cn } from "@/lib/utils"

interface SheetShellProps {
    title: string
    description?: string
    children: React.ReactNode
    open: boolean
    onOpenChange: (open: boolean) => void
    size?: "default" | "sm" | "lg" | "xl" | "full"
}

const sizeClasses: Record<string, string> = {
    sm: "data-[side=right]:sm:!max-w-sm data-[side=left]:sm:!max-w-sm",
    default: "data-[side=right]:sm:!max-w-md data-[side=left]:sm:!max-w-md",
    lg: "data-[side=right]:sm:!max-w-lg data-[side=left]:sm:!max-w-lg data-[side=right]:lg:!max-w-xl data-[side=left]:lg:!max-w-xl",
    xl: "data-[side=right]:sm:!max-w-xl data-[side=left]:sm:!max-w-xl data-[side=right]:lg:!max-w-2xl data-[side=left]:lg:!max-w-2xl",
    full: "data-[side=right]:sm:!max-w-full data-[side=left]:sm:!max-w-full",
}

export function SheetShell({
    title,
    description,
    children,
    open,
    onOpenChange,
    size = "default",
}: SheetShellProps) {
    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent
                side="right"
                className={cn(
                    "w-full flex flex-col h-full bg-background p-0 border-l data-[state=closed]:slide-out-to-right data-[state=open]:slide-in-from-right",
                    sizeClasses[size] ?? sizeClasses.default
                )}
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
                    <div className="flex-1 overflow-y-auto px-6 py-4">
                        {children}
                    </div>
                </div>
            </SheetContent>
        </Sheet>
    )
}

