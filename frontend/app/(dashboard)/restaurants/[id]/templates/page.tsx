"use client"

import { useState } from "react"
import { Pencil, Plus, PowerOff, RotateCcw, Search, Shapes } from "lucide-react"
import { useSetTemplateActive, useTemplates } from "@/lib/hooks/use-templates"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { SheetShell } from "@/components/shared/sheet-shell"
import { TemplateForm } from "@/components/modules/templates/template-form"
import { Can } from "@/components/shared/can"
import { PermissionAction, PermissionModule } from "@/lib/permissions"
import { formatCurrency } from "@/lib/formatters"
import { TemplateResponse } from "@/lib/api-types"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { ListPagination } from "@/components/shared/list-pagination"
import { useDebounce } from "@/lib/hooks/use-debounce"

export default function TemplatesPage() {
    const [search, setSearch] = useState("")
    const [editingId, setEditingId] = useState<string | null>(null)
    const [isFormOpen, setIsFormOpen] = useState(false)
    const [activationTarget, setActivationTarget] = useState<TemplateResponse | null>(null)
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const debouncedSearch = useDebounce(search, 300)
    const { data, isLoading } = useTemplates({ search: debouncedSearch, page, size })
    const templates = data?.content ?? []
    const setActive = useSetTemplateActive()

    const activationBlocked = !!activationTarget?.isActive && activationTarget.isPublished

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Armables</h1>
                    <p className="mt-1 text-muted-foreground">Plantillas configurables con grupos de seleccion.</p>
                </div>
                <Can I={PermissionAction.CREATE} a={PermissionModule.TEMPLATES}>
                    <Button onClick={() => { setEditingId(null); setIsFormOpen(true) }}><Plus className="mr-2 h-4 w-4" />Nuevo armable</Button>
                </Can>
            </div>

            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input className="pl-9" placeholder="Buscar armable..." value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} />
            </div>
            {isLoading ? (
                <div className="space-y-2">{[1, 2, 3].map((item) => <Skeleton key={item} className="h-14 w-full" />)}</div>
            ) : templates.length === 0 ? (
                <div className="flex flex-col items-center justify-center gap-3 border border-dashed bg-muted/20 py-20 text-center">
                    <Shapes className="h-10 w-10 text-muted-foreground/40" />
                    <p className="font-medium">{search ? "Sin resultados" : "No hay armables registrados"}</p>
                </div>
            ) : (
                <div className="overflow-hidden border">
                    <table className="w-full text-sm">
                        <thead><tr className="border-b bg-muted/50 text-muted-foreground">
                            <th className="h-11 px-4 text-left font-medium">Nombre</th>
                            <th className="h-11 px-4 text-right font-medium">Precio base</th>
                            <th className="h-11 px-4 text-center font-medium">Slots</th>
                            <th className="h-11 px-4 text-center font-medium">Estado</th>
                            <th className="h-11 px-4 text-center font-medium">Acciones</th>
                        </tr></thead>
                        <tbody>{templates.map((template) => (
                            <tr key={template.id} className="border-b last:border-0 hover:bg-muted/30">
                                <td className="p-4 font-medium">{template.name}</td>
                                <td className="p-4 text-right font-mono">{formatCurrency(template.basePrice)}</td>
                                <td className="p-4 text-center">{template.slots.length}</td>
                                <td className="p-4 text-center">
                                    {!template.isActive ? <Badge variant="destructive">Inactivo</Badge> : template.isPublished ? <Badge>Publicado</Badge> : <Badge variant="outline">Sin publicar</Badge>}
                                </td>
                                <td className="p-4"><div className="flex justify-center gap-1">
                                    <Can I={PermissionAction.EDIT} a={PermissionModule.TEMPLATES}>
                                        <Button variant="ghost" size="icon" title="Editar" onClick={() => { setEditingId(template.id); setIsFormOpen(true) }}><Pencil className="h-4 w-4" /></Button>
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            className={template.isActive ? "text-destructive hover:text-destructive" : "text-emerald-600 hover:text-emerald-700"}
                                            title={template.isActive ? "Desactivar" : "Reactivar"}
                                            onClick={() => setActivationTarget(template)}
                                        >
                                            {template.isActive ? <PowerOff className="h-4 w-4" /> : <RotateCcw className="h-4 w-4" />}
                                        </Button>
                                    </Can>
                                </div></td>
                            </tr>
                        ))}</tbody>
                    </table>
                </div>
            )}
            <ListPagination
                totalElements={data?.totalElements ?? 0}
                totalPages={data?.totalPages ?? 0}
                page={page}
                size={size}
                label="armable(s)"
                onPageChange={setPage}
                onSizeChange={(nextSize) => { setSize(nextSize); setPage(0) }}
            />

            <SheetShell open={isFormOpen} onOpenChange={setIsFormOpen} size="xl" title={editingId ? "Editar armable" : "Nuevo armable"} description="Configura slots y opciones. Publicarlo en el menu es una operacion independiente.">
                <div className="mt-6"><TemplateForm templateId={editingId} onSuccess={() => setIsFormOpen(false)} /></div>
            </SheetShell>
            <ConfirmDialog
                open={!!activationTarget}
                onOpenChange={(open) => {
                    if (!open) setActivationTarget(null)
                }}
                title={activationBlocked ? "Armable publicado" : activationTarget?.isActive ? "Desactivar armable" : "Reactivar armable"}
                description={activationBlocked
                    ? `No puedes desactivar "${activationTarget?.name ?? ""}" mientras este publicado. Retiralo primero del submenu.`
                    : activationTarget?.isActive
                        ? `El armable "${activationTarget.name}" dejara de estar disponible para nuevas operaciones.`
                        : `El armable "${activationTarget?.name ?? ""}" volvera a estar disponible para nuevas operaciones. Reactivarlo no lo publica automaticamente.`}
                confirmText={activationBlocked ? "Entendido" : activationTarget?.isActive ? "Desactivar" : "Reactivar"}
                cancelText="Cancelar"
                variant={activationTarget?.isActive ? "destructive" : "default"}
                onConfirm={async () => {
                    if (!activationTarget || activationBlocked) return
                    await setActive.mutateAsync({ id: activationTarget.id, isActive: !activationTarget.isActive })
                }}
            />
        </div>
    )
}
