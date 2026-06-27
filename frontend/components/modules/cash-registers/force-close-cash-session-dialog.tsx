"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2 } from "lucide-react"
import { useForm } from "react-hook-form"

import { DialogShell } from "@/components/shared/dialog-shell"
import { Button } from "@/components/ui/button"
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { CashSessionListResponse } from "@/lib/api-types"
import { formatPriceDisplay, parsePriceInput } from "@/lib/formatters"
import { useForceCloseCashSession } from "@/lib/hooks/use-cash-sessions"

import { closeCashSessionSchema, CloseCashSessionFormValues } from "./cash-session-schema"

interface ForceCloseCashSessionDialogProps {
    session: CashSessionListResponse | null
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function ForceCloseCashSessionDialog({
    session,
    open,
    onOpenChange,
}: ForceCloseCashSessionDialogProps) {
    const forceCloseSession = useForceCloseCashSession()
    const form = useForm<CloseCashSessionFormValues>({
        resolver: zodResolver(closeCashSessionSchema),
        defaultValues: {
            countedCash: "",
            differenceReason: "",
            notes: "",
        },
    })

    const handleOpenChange = (isOpen: boolean) => {
        if (!isOpen) form.reset()
        onOpenChange(isOpen)
    }

    const handleSubmit = (values: CloseCashSessionFormValues) => {
        if (!session) return
        forceCloseSession.mutate(
            {
                restaurantId: session.restaurantId,
                sessionId: session.id,
                request: {
                    countedCash: parsePriceInput(values.countedCash),
                    differenceReason: values.differenceReason.trim() || undefined,
                    notes: values.notes.trim() || undefined,
                },
            },
            { onSuccess: () => handleOpenChange(false) }
        )
    }

    return (
        <DialogShell
            open={open}
            onOpenChange={handleOpenChange}
            title="Force close cash session"
            description={session
                ? `${session.restaurantName} - ${session.cashRegisterName}. Emergency action for managers or owners. The assigned device will be released.`
                : undefined}
        >
            <Form {...form}>
                <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
                    <FormField
                        control={form.control}
                        name="countedCash"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Efectivo contado</FormLabel>
                                <FormControl>
                                    <Input
                                        inputMode="decimal"
                                        placeholder="0"
                                        {...field}
                                        onBlur={(event) => {
                                            field.onBlur()
                                            field.onChange(formatPriceDisplay(event.target.value))
                                        }}
                                    />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                    <FormField
                        control={form.control}
                        name="differenceReason"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Explicacion de diferencia</FormLabel>
                                <FormControl>
                                    <Textarea
                                        placeholder="Obligatoria cuando el conteo no coincide"
                                        className="resize-none"
                                        {...field}
                                    />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                    <FormField
                        control={form.control}
                        name="notes"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Notes</FormLabel>
                                <FormControl>
                                    <Textarea placeholder="Reason for force close" className="resize-none" {...field} />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                    <div className="flex justify-end pt-2">
                        <Button type="submit" variant="destructive" disabled={forceCloseSession.isPending}>
                            {forceCloseSession.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                            Force close
                        </Button>
                    </div>
                </form>
            </Form>
        </DialogShell>
    )
}
