"use client"

import * as React from "react"
import {
    AlertDialog,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Loader2 } from "lucide-react"

interface ConfirmDialogProps {
    title: string
    description: React.ReactNode
    open: boolean
    onOpenChange: (open: boolean) => void
    onConfirm: () => Promise<void> | void
    variant?: "default" | "destructive"
    confirmText?: string
    cancelText?: string
}

export function ConfirmDialog({
    title,
    description,
    open,
    onOpenChange,
    onConfirm,
    variant = "default",
    confirmText = "Continue",
    cancelText = "Cancel"
}: ConfirmDialogProps) {
    const [isLoading, setIsLoading] = React.useState(false)

    const handleConfirm = async (e: React.MouseEvent) => {
        e.preventDefault()
        setIsLoading(true)
        try {
            await onConfirm()
            onOpenChange(false)
        } catch (error) {
            // Error handling should be done in the parent or a global toaster
            console.error(error)
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <AlertDialog open={open} onOpenChange={onOpenChange}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>{title}</AlertDialogTitle>
                    <AlertDialogDescription>
                        {description}
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel disabled={isLoading}>
                        {cancelText}
                    </AlertDialogCancel>
                    <Button
                        variant={variant === "destructive" ? "destructive" : "default"}
                        onClick={handleConfirm}
                        disabled={isLoading}
                    >
                        {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                        {confirmText}
                    </Button>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    )
}
