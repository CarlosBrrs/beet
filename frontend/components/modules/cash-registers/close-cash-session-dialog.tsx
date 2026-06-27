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
import { CashSessionResponse } from "@/lib/api-types"
import { formatPriceDisplay, parsePriceInput } from "@/lib/formatters"
import { useCloseCashSession } from "@/lib/hooks/use-cash-sessions"
import { useCashSessionReconciliation } from "@/lib/hooks/use-cash-operations"
import { formatCurrency } from "@/lib/formatters"

import { closeCashSessionSchema, CloseCashSessionFormValues } from "./cash-session-schema"

interface CloseCashSessionDialogProps {
    session: CashSessionResponse
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function CloseCashSessionDialog({ session, open, onOpenChange }: CloseCashSessionDialogProps) {
    const closeSession = useCloseCashSession()
    const { data: reconciliation } = useCashSessionReconciliation(session.id, open)
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
        closeSession.mutate(
            {
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
            title="Close cash session"
        >
            <Form {...form}>
                <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
                    <div className="border bg-muted/30 p-3 text-sm">
                        {reconciliation?.blind ? (
                            <p>Conteo ciego: registra el efectivo contado antes de ver el esperado.</p>
                        ) : (
                            <div className="flex items-center justify-between">
                                <span className="text-muted-foreground">Efectivo esperado</span>
                                <strong>{formatCurrency(Number(reconciliation?.expectedCash ?? 0))}</strong>
                            </div>
                        )}
                    </div>
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
                                    <Textarea placeholder="Optional notes" className="resize-none" {...field} />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    <div className="flex justify-end pt-2">
                        <Button type="submit" disabled={closeSession.isPending}>
                            {closeSession.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                            Close session
                        </Button>
                    </div>
                </form>
            </Form>
        </DialogShell>
    )
}
