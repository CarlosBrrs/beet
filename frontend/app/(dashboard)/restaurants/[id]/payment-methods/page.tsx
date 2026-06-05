"use client"

import { CreditCard, Plus } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"
import { useCreatePaymentMethod, usePaymentMethods, useUpdatePaymentMethod } from "@/lib/hooks/use-orders"
import { PaymentMethodRequest } from "@/lib/api-types"

const defaults: PaymentMethodRequest[] = [
    { code: "CASH", name: "Efectivo", type: "CASH", isActive: true, requiresReference: false, sortOrder: 10 },
    { code: "DEBIT", name: "Tarjeta debito", type: "DEBIT_CARD", isActive: true, requiresReference: true, sortOrder: 20 },
    { code: "CREDIT", name: "Tarjeta credito", type: "CREDIT_CARD", isActive: true, requiresReference: true, sortOrder: 30 },
    { code: "NEQUI", name: "Nequi", type: "NEQUI", isActive: true, requiresReference: true, sortOrder: 40 },
    { code: "DAVIPLATA", name: "Daviplata", type: "DAVIPLATA", isActive: true, requiresReference: true, sortOrder: 50 },
]

export default function PaymentMethodsPage() {
    const { data, isLoading } = usePaymentMethods()
    const createMethod = useCreatePaymentMethod()
    const updateMethod = useUpdatePaymentMethod()
    const methods = data ?? []

    const seedDefaults = async () => {
        const existingCodes = new Set(methods.map((method) => method.code))
        for (const method of defaults) {
            if (!existingCodes.has(method.code!)) {
                await createMethod.mutateAsync(method)
            }
        }
    }

    return (
        <div className="space-y-6 pb-10">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Payment Methods</h1>
                    <p className="mt-1 text-muted-foreground">Metodos permitidos para cobrar ordenes en este restaurante.</p>
                </div>
                <Button onClick={seedDefaults} disabled={createMethod.isPending}>
                    <Plus className="mr-2 h-4 w-4" />
                    Crear defaults
                </Button>
            </div>

            <div className="overflow-hidden border">
                <table className="w-full text-sm">
                    <thead>
                        <tr className="border-b bg-muted/50 text-muted-foreground">
                            <th className="h-11 px-4 text-left font-medium">Metodo</th>
                            <th className="h-11 px-4 text-center font-medium">Tipo</th>
                            <th className="h-11 px-4 text-center font-medium">Referencia</th>
                            <th className="h-11 px-4 text-center font-medium">Activo</th>
                        </tr>
                    </thead>
                    <tbody>
                        {isLoading ? (
                            <tr><td colSpan={4} className="p-8 text-center text-muted-foreground">Cargando metodos...</td></tr>
                        ) : methods.length === 0 ? (
                            <tr>
                                <td colSpan={4} className="p-12 text-center">
                                    <CreditCard className="mx-auto h-8 w-8 text-muted-foreground/50" />
                                    <p className="mt-3 text-sm text-muted-foreground">No hay metodos configurados. Crea los defaults para probar pagos.</p>
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
                                <td className="p-4 text-center">
                                    <Switch
                                        checked={method.isActive}
                                        onCheckedChange={(checked) => updateMethod.mutate({
                                            methodId: method.id,
                                            request: { isActive: checked },
                                        })}
                                    />
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}
