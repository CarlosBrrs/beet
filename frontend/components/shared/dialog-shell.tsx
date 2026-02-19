"use client"

import * as React from "react"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"

interface DialogShellProps {
    title: string
    description?: string
    children: React.ReactNode
    open: boolean
    onOpenChange: (open: boolean) => void
    size?: "sm" | "default" | "lg" | "xl"
}

export function DialogShell({
    title,
    description,
    children,
    open,
    onOpenChange,
    size = "default"
}: DialogShellProps) {
    const sizeClasses = {
        sm: "sm:max-w-[400px]",
        default: "sm:max-w-[500px]",
        lg: "sm:max-w-[600px]",
        xl: "sm:max-w-[800px]"
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className={sizeClasses[size]}>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                    {description && (
                        <DialogDescription>
                            {description}
                        </DialogDescription>
                    )}
                </DialogHeader>
                <div className="py-4">
                    {children}
                </div>
            </DialogContent>
        </Dialog>
    )
}
