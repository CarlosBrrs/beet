"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2 } from "lucide-react"
import { useMemo } from "react"
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
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { getOrCreateDeviceId } from "@/lib/device-id"
import { formatPriceDisplay, parsePriceInput } from "@/lib/formatters"
import { useCashRegisters } from "@/lib/hooks/use-cash-registers"
import { useOpenCashSession } from "@/lib/hooks/use-cash-sessions"

import { openCashSessionSchema, OpenCashSessionFormValues } from "./cash-session-schema"

interface OpenCashSessionDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
}

export function OpenCashSessionDialog({ open, onOpenChange }: OpenCashSessionDialogProps) {
    const deviceId = getOrCreateDeviceId()
    const { data: registers = [], isLoading } = useCashRegisters()
    const openSession = useOpenCashSession()
    const form = useForm<OpenCashSessionFormValues>({
        resolver: zodResolver(openCashSessionSchema),
        defaultValues: {
            cashRegisterId: "",
            openingAmount: "",
            notes: "",
        },
    })

    const availableRegisters = useMemo(
        () => registers.filter((register) =>
            register.isActive && (!register.deviceId || register.deviceId === deviceId)
        ),
        [deviceId, registers]
    )

    const handleOpenChange = (isOpen: boolean) => {
        if (!isOpen) form.reset()
        onOpenChange(isOpen)
    }

    const handleSubmit = (values: OpenCashSessionFormValues) => {
        openSession.mutate(
            {
                cashRegisterId: values.cashRegisterId,
                openingAmount: parsePriceInput(values.openingAmount),
                notes: values.notes.trim() || undefined,
            },
            { onSuccess: () => handleOpenChange(false) }
        )
    }

    return (
        <DialogShell
            open={open}
            onOpenChange={handleOpenChange}
            title="Open cash session"
        >
            <Form {...form}>
                <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
                    <FormField
                        control={form.control}
                        name="cashRegisterId"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Cash register</FormLabel>
                                <Select value={field.value} onValueChange={field.onChange} disabled={isLoading}>
                                    <FormControl>
                                        <SelectTrigger className="w-full">
                                            <SelectValue placeholder={isLoading ? "Loading..." : "Select a cash register"} />
                                        </SelectTrigger>
                                    </FormControl>
                                    <SelectContent>
                                        {availableRegisters.map((register) => (
                                            <SelectItem key={register.id} value={register.id}>
                                                {register.name}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                                <FormMessage />
                            </FormItem>
                        )}
                    />

                    {availableRegisters.length === 0 && !isLoading && (
                        <p className="border border-dashed p-3 text-xs text-muted-foreground">
                            No active cash register is available for this device.
                        </p>
                    )}

                    <FormField
                        control={form.control}
                        name="openingAmount"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Opening amount</FormLabel>
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
                        <Button
                            type="submit"
                            disabled={openSession.isPending || availableRegisters.length === 0}
                        >
                            {openSession.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
                            Open session
                        </Button>
                    </div>
                </form>
            </Form>
        </DialogShell>
    )
}
