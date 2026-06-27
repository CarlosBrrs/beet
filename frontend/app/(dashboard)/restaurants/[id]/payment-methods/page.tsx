"use client"

import { CreditCard, Loader2, Pencil, Plus } from "lucide-react"
import { useState } from "react"

import { Can } from "@/components/shared/can"
import { DialogShell } from "@/components/shared/dialog-shell"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { PaymentMethodResponse, PaymentMethodType } from "@/lib/api-types"
import { useCreatePaymentMethod, usePaymentMethods, useUpdatePaymentMethod } from "@/lib/hooks/use-orders"

const methodTypes: PaymentMethodType[] = [
    "CASH",
    "DEBIT_CARD",
    "CREDIT_CARD",
    "NEQUI",
    "DAVIPLATA",
    "INTERNAL_CREDIT",
    "OTHER",
]

export default function PaymentMethodsPage() {
    const { data, isLoading } = usePaymentMethods()
    const createMethod = useCreatePaymentMethod()
    const updateMethod = useUpdatePaymentMethod()
    const methods = data ?? []
    const [editing, setEditing] = useState<PaymentMethodResponse | null>(null)
    const [createOpen, setCreateOpen] = useState(false)
    const [deactivateTarget, setDeactivateTarget] = useState<PaymentMethodResponse | null>(null)
    const [code, setCode] = useState("")
    const [name, setName] = useState("")
    const [type, setType] = useState<PaymentMethodType>("OTHER")
    const [requiresReference, setRequiresReference] = useState(false)
    const [sortOrder, setSortOrder] = useState("0")

    const openEdit = (method: PaymentMethodResponse) => {
        setEditing(method)
        setName(method.name)
        setRequiresReference(method.requiresReference)
        setSortOrder(String(method.sortOrder))
    }

    const reset = () => {
        setCode("")
        setName("")
        setType("OTHER")
        setRequiresReference(false)
        setSortOrder("0")
        setEditing(null)
    }

    return (
        <div className="space-y-6 pb-10">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Payment Methods</h1>
                    <p className="mt-1 text-muted-foreground">
                        Configura los medios que el restaurante acepta al cobrar.
                    </p>
                </div>
                <Can I="CREATE" a="PAYMENTS">
                    <Button onClick={() => setCreateOpen(true)}>
                        <Plus className="h-4 w-4" />
                        Nuevo metodo
                    </Button>
                </Can>
            </div>

            <div className="overflow-hidden border">
                <table className="w-full text-sm">
                    <thead>
                        <tr className="border-b bg-muted/50 text-muted-foreground">
                            <th className="h-11 px-4 text-left font-medium">Metodo</th>
                            <th className="h-11 px-4 text-center font-medium">Tipo</th>
                            <th className="h-11 px-4 text-center font-medium">Referencia</th>
                            <th className="h-11 px-4 text-center font-medium">Orden</th>
                            <th className="h-11 px-4 text-center font-medium">Activo</th>
                            <th className="h-11 w-12" />
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr>
                                <td colSpan={6} className="p-8 text-center">
                                    <Loader2 className="mx-auto h-5 w-5 animate-spin" />
                                </td>
                            </tr>
                        ) : methods.length === 0 ? (
                            <tr>
                                <td colSpan={6} className="p-12 text-center">
                                    <CreditCard className="mx-auto h-8 w-8 text-muted-foreground/50" />
                                    <p className="mt-3 text-sm text-muted-foreground">
                                        No hay metodos configurados. Revisa la migracion V22.
                                    </p>
                                </td>
                            </tr>
                        ) : methods.map((method) => (
                            <tr key={method.id} className="border-b last:border-0">
                                <td className="p-4">
                                    <p className="font-medium">{method.name}</p>
                                    <p className="text-xs text-muted-foreground">{method.code}</p>
                                </td>
                                <td className="p-4 text-center"><Badge variant="outline">{method.type}</Badge></td>
                                <td className="p-4 text-center">{method.requiresReference ? "Requerida" : "Opcional"}</td>
                                <td className="p-4 text-center">{method.sortOrder}</td>
                                <td className="p-4 text-center">
                                    <Can I="EDIT" a="PAYMENTS">
                                        <Switch
                                            checked={method.isActive}
                                            onCheckedChange={(checked) => {
                                                if (!checked) {
                                                    setDeactivateTarget(method)
                                                    return
                                                }
                                                updateMethod.mutate({
                                                    methodId: method.id,
                                                    request: { isActive: true },
                                                })
                                            }}
                                        />
                                    </Can>
                                </td>
                                <td className="p-2">
                                    <Can I="EDIT" a="PAYMENTS">
                                        <Button variant="ghost" size="icon" title="Editar" onClick={() => openEdit(method)}>
                                            <Pencil className="h-4 w-4" />
                                        </Button>
                                    </Can>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <DialogShell
                open={createOpen || !!editing}
                onOpenChange={(open) => {
                    if (!open) {
                        setCreateOpen(false)
                        reset()
                    }
                }}
                title={editing ? "Editar metodo de pago" : "Crear metodo de pago"}
            >
                <div className="space-y-4">
                    {!editing && (
                        <>
                            <Input placeholder="Codigo, ej. SODEXO" value={code} onChange={(event) => setCode(event.target.value)} />
                            <Select value={type} onValueChange={(value) => setType(value as PaymentMethodType)}>
                                <SelectTrigger><SelectValue /></SelectTrigger>
                                <SelectContent>
                                    {methodTypes.map((item) => <SelectItem key={item} value={item}>{item}</SelectItem>)}
                                </SelectContent>
                            </Select>
                        </>
                    )}
                    <Input placeholder="Nombre visible" value={name} onChange={(event) => setName(event.target.value)} />
                    <Input
                        type="number"
                        min={0}
                        placeholder="Orden"
                        value={sortOrder}
                        onChange={(event) => setSortOrder(event.target.value)}
                    />
                    <label className="flex items-center justify-between border p-3 text-sm">
                        Exigir referencia
                        <Switch checked={requiresReference} onCheckedChange={setRequiresReference} />
                    </label>
                    <div className="flex justify-end">
                        <Button
                            disabled={!name.trim() || (!editing && !code.trim())
                                || createMethod.isPending || updateMethod.isPending}
                            onClick={() => {
                                if (editing) {
                                    updateMethod.mutate({
                                        methodId: editing.id,
                                        request: {
                                            name: name.trim(),
                                            requiresReference,
                                            sortOrder: Number(sortOrder),
                                        },
                                    }, { onSuccess: () => reset() })
                                } else {
                                    createMethod.mutate({
                                        code,
                                        name: name.trim(),
                                        type,
                                        isActive: true,
                                        requiresReference,
                                        sortOrder: Number(sortOrder),
                                    }, {
                                        onSuccess: () => {
                                            setCreateOpen(false)
                                            reset()
                                        },
                                    })
                                }
                            }}
                        >
                            Guardar
                        </Button>
                    </div>
                </div>
            </DialogShell>

            <DialogShell
                open={!!deactivateTarget}
                onOpenChange={(open) => {
                    if (!open) setDeactivateTarget(null)
                }}
                title="Desactivar metodo"
                description="No aparecera en nuevos cobros. El historial de pagos conserva su referencia."
            >
                <div className="flex justify-end gap-2">
                    <Button variant="outline" onClick={() => setDeactivateTarget(null)}>Cancelar</Button>
                    <Button
                        variant="destructive"
                        disabled={updateMethod.isPending}
                        onClick={() => deactivateTarget && updateMethod.mutate({
                            methodId: deactivateTarget.id,
                            request: { isActive: false },
                        }, { onSuccess: () => setDeactivateTarget(null) })}
                    >
                        Desactivar
                    </Button>
                </div>
            </DialogShell>
        </div>
    )
}
