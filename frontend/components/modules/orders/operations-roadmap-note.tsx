import { AlertTriangle, CheckSquare, Clock3, CreditCard, Split } from "lucide-react"

export function OperationsRoadmapNote() {
    return (
        <div className="border border-amber-300 bg-amber-50/70 p-4 text-sm text-amber-950 dark:border-amber-900 dark:bg-amber-950/20 dark:text-amber-100">
            <div className="flex items-start gap-3">
                <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0" />
                <div className="w-full space-y-5">
                    <div>
                        <h2 className="font-semibold">Validacion operativa pendiente</h2>
                        <p className="mt-1 text-xs leading-relaxed">
                            Estos puntos deben probarse antes de considerar cerrado el flujo de ordenes y caja.
                        </p>
                    </div>

                    <section>
                        <h3 className="mb-2 flex items-center gap-2 font-semibold">
                            <Clock3 className="h-4 w-4" />
                            V21: prepago, vencimientos y reservas
                        </h3>
                        <ul className="list-disc space-y-1 pl-5 text-xs leading-relaxed">
                            <li>Probar reservas, vencimiento, pagos parciales y liberacion automatica.</li>
                            <li>Reactivar con y sin stock, verificando que no queden cambios parciales.</li>
                            <li>Agregar lotes a ordenes OPEN y validar un ticket nuevo por envio.</li>
                            <li>Validar PARTIALLY_READY cuando conviven tickets listos y pendientes.</li>
                        </ul>
                    </section>

                    <section>
                        <h3 className="mb-2 flex items-center gap-2 font-semibold">
                            <Split className="h-4 w-4" />
                            Diferido: cuentas divididas
                        </h3>
                        <ul className="list-disc space-y-1 pl-5 text-xs leading-relaxed">
                            <li>Dividir por items/cantidades, monto, porcentaje o partes iguales.</li>
                            <li>Cobrar cada cuenta y mostrar saldo individual y total.</li>
                            <li>Asignar pagos y propinas sin exceder cantidades ni total.</li>
                            <li>Definir recalculo cuando cambian items despues de dividir.</li>
                        </ul>
                    </section>

                    <section>
                        <h3 className="mb-2 flex items-center gap-2 font-semibold">
                            <CreditCard className="h-4 w-4" />
                            V22: pagos y cierre de caja
                        </h3>
                        <ul className="list-disc space-y-1 pl-5 text-xs leading-relaxed">
                            <li>Probar defaults backend, metodos personalizados y bloqueo del ultimo metodo activo.</li>
                            <li>Validar movimientos IN/OUT y anulacion solo antes del arqueo.</li>
                            <li>Probar arqueo BLIND/VISIBLE, contado, esperado, diferencia y snapshots.</li>
                            <li>Probar cierre diario estricto, reapertura y conservacion de cierres anteriores.</li>
                        </ul>
                    </section>

                    <section>
                        <h3 className="mb-2 flex items-center gap-2 font-semibold">
                            <CreditCard className="h-4 w-4" />
                            Diferido: devoluciones completas
                        </h3>
                        <ul className="list-disc space-y-1 pl-5 text-xs leading-relaxed">
                            <li>Elegir el metodo real usado para devolver.</li>
                            <li>Diferenciar devolucion, anulacion y reverso de pasarela.</li>
                            <li>Definir autorizaciones, limites e impacto sobre caja y dia.</li>
                            <li>Conciliar con proveedores externos.</li>
                            <li>Regla temporal V22: descontar del metodo del pago original.</li>
                        </ul>
                    </section>

                    <p className="flex items-center gap-2 text-xs font-medium">
                        <CheckSquare className="h-4 w-4" />
                        Guias: ORDERS_POS_TESTING.md y CASH_OPERATIONS_TESTING.md.
                    </p>
                </div>
            </div>
        </div>
    )
}
