"use client"

import { useState } from "react"
import { Check, Eye, Info, PackageSearch, Pencil, Plus, PowerOff, RotateCcw, Search, X } from "lucide-react"
import { useProductDependencies, useProducts, useSetProductActive } from "@/lib/hooks/use-items"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { SheetShell } from "@/components/shared/sheet-shell"
import { Can } from "@/components/shared/can"
import { ProductHubDetail } from "@/components/modules/products/product-hub-detail"
import { ProductForm } from "@/components/modules/products/product-form"
import { PermissionAction, PermissionModule } from "@/lib/permissions"
import { formatCurrency } from "@/lib/formatters"
import { useDebounce } from "@/lib/hooks/use-debounce"
import { ItemResponse } from "@/lib/api-types"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { ListPagination } from "@/components/shared/list-pagination"

function CatalogBadge({ product }: { product: ItemResponse }) {
    if (!product.isActive) return <Badge variant="destructive">Inactivo</Badge>
    if (product.isPublished && product.isUsedAsTemplateOption) return <Badge>Publicado + opcion</Badge>
    if (product.isPublished) return <Badge>Publicado</Badge>
    if (product.isUsedAsTemplateOption) return <Badge variant="secondary">Solo opcion</Badge>
    return <Badge variant="outline">Sin uso</Badge>
}

export default function ProductsPage() {
    const [search, setSearch] = useState("")
    const [detailProductId, setDetailProductId] = useState<string | null>(null)
    const [editingProductId, setEditingProductId] = useState<string | null>(null)
    const [isDetailOpen, setIsDetailOpen] = useState(false)
    const [isFormOpen, setIsFormOpen] = useState(false)
    const [activationTarget, setActivationTarget] = useState<ItemResponse | null>(null)
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const debouncedSearch = useDebounce(search, 300)
    const { data, isLoading } = useProducts({ search: debouncedSearch, page, size })
    const setProductActive = useSetProductActive()
    const products = data?.content ?? []
    const activationBlocked = !!activationTarget?.isActive
        && (activationTarget.isPublished || activationTarget.isUsedAsTemplateOption)
    const { data: dependencies, isLoading: isLoadingDependencies } = useProductDependencies(
        activationBlocked ? activationTarget?.id : undefined
    )

    const activationDescription = activationTarget?.isActive
        ? activationBlocked
            ? (
                <div className="space-y-3">
                    <p>No puedes desactivar <strong>{activationTarget.name}</strong> mientras conserve vínculos.</p>
                    {isLoadingDependencies ? (
                        <p>Consultando ubicaciones...</p>
                    ) : (
                        <>
                            {!!dependencies?.publications.length && (
                                <div>
                                    <p className="font-medium text-foreground">Publicado en:</p>
                                    {dependencies.publications.map((publication) => (
                                        <p key={publication.submenuId}>{publication.menuName} / {publication.submenuName}</p>
                                    ))}
                                </div>
                            )}
                            {!!dependencies?.templateUsages.length && (
                                <div>
                                    <p className="font-medium text-foreground">Usado en armables:</p>
                                    {dependencies.templateUsages.map((usage) => (
                                        <p key={usage.slotId}>{usage.templateName} / {usage.slotName}</p>
                                    ))}
                                </div>
                            )}
                        </>
                    )}
                </div>
            )
            : `El producto "${activationTarget.name}" dejara de estar disponible para nuevas operaciones.`
        : `El producto "${activationTarget?.name ?? ""}" volvera a estar disponible para nuevas operaciones. Reactivarlo no lo publica ni lo habilita automaticamente como opcion de armable.`

    const openCreate = () => {
        setEditingProductId(null)
        setIsFormOpen(true)
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Productos</h1>
                    <p className="mt-1 text-muted-foreground">Catalogo comercial reutilizable del restaurante.</p>
                </div>
                <Can I={PermissionAction.CREATE} a={PermissionModule.PRODUCTS}>
                    <Button onClick={openCreate}><Plus className="mr-2 h-4 w-4" />Nuevo producto</Button>
                </Can>
            </div>

            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input placeholder="Buscar producto..." className="pl-9" value={search} onChange={(event) => { setSearch(event.target.value); setPage(0) }} />
            </div>

            <div className="flex max-w-4xl gap-3 border bg-muted/30 p-3 text-sm text-muted-foreground">
                <Info className="mt-0.5 h-4 w-4 shrink-0" />
                <p>
                    La publicacion individual y el uso en armables son independientes. Un producto aparece en el menu solo al publicarlo desde un submenu. Para usarlo dentro de slots, habilita la opcion de armables al editarlo.
                </p>
            </div>
            {isLoading ? (
                <div className="space-y-2">{[1, 2, 3, 4].map((item) => <Skeleton key={item} className="h-14 w-full" />)}</div>
            ) : products.length === 0 ? (
                <div className="flex flex-col items-center justify-center gap-3 border border-dashed bg-muted/20 py-20 text-center">
                    <PackageSearch className="h-10 w-10 text-muted-foreground/40" />
                    <div>
                        <p className="font-medium">{search ? "Sin resultados para tu busqueda" : "No hay productos registrados"}</p>
                        <p className="mt-1 text-sm text-muted-foreground">Crea productos aqui y publicalos desde el submenu correspondiente.</p>
                    </div>
                </div>
            ) : (
                <div className="overflow-hidden border">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="border-b bg-muted/50 text-muted-foreground">
                                <th className="h-11 px-4 text-left font-medium">Nombre</th>
                                <th className="h-11 px-4 text-right font-medium">Precio venta</th>
                                <th className="h-11 px-4 text-right font-medium">Costo teorico</th>
                                <th className="h-11 px-4 text-center font-medium">Tipo</th>
                                <th className="h-11 px-4 text-center font-medium">Opcion de armable</th>
                                <th className="h-11 px-4 text-center font-medium">Uso comercial</th>
                                <th className="h-11 px-4 text-center font-medium">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            {products.map((product) => (
                                <tr key={product.id} className="border-b transition-colors last:border-0 hover:bg-muted/30">
                                    <td className="p-4 font-medium">
                                        {product.name}
                                        {product.description && <p className="mt-0.5 max-w-[240px] truncate text-xs font-normal text-muted-foreground">{product.description}</p>}
                                    </td>
                                    <td className="p-4 text-right font-mono">{product.salePrice ? formatCurrency(product.salePrice) : "-"}</td>
                                    <td className="p-4 text-right font-mono text-muted-foreground">{product.theoreticalCost ? formatCurrency(product.theoreticalCost) : "-"}</td>
                                    <td className="p-4 text-center"><Badge variant="outline">{product.isInventoryTracked ? "Rastreado" : "Plano"}</Badge></td>
                                    <td className="p-4 text-center">
                                        {product.isAvailableAsTemplateOption
                                            ? <Check aria-label="Habilitado como opcion de armable" className="mx-auto h-4 w-4 text-emerald-600" />
                                            : <X aria-label="No habilitado como opcion de armable" className="mx-auto h-4 w-4 text-muted-foreground" />}
                                    </td>
                                    <td className="p-4 text-center"><CatalogBadge product={product} /></td>
                                    <td className="p-4">
                                        <div className="flex justify-center gap-1">
                                            <Button variant="ghost" size="icon" title="Ver" onClick={() => { setDetailProductId(product.id); setIsDetailOpen(true) }}><Eye className="h-4 w-4" /></Button>
                                            <Can I={PermissionAction.EDIT} a={PermissionModule.PRODUCTS}>
                                                <Button variant="ghost" size="icon" title="Editar" onClick={() => { setEditingProductId(product.id); setIsFormOpen(true) }}><Pencil className="h-4 w-4" /></Button>
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className={product.isActive ? "text-destructive hover:text-destructive" : "text-emerald-600 hover:text-emerald-700"}
                                                    title={product.isActive ? "Desactivar" : "Reactivar"}
                                                    onClick={() => setActivationTarget(product)}
                                                >
                                                    {product.isActive ? <PowerOff className="h-4 w-4" /> : <RotateCcw className="h-4 w-4" />}
                                                </Button>
                                            </Can>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            <ListPagination
                totalElements={data?.totalElements ?? 0}
                totalPages={data?.totalPages ?? 0}
                page={page}
                size={size}
                label="producto(s)"
                onPageChange={setPage}
                onSizeChange={(nextSize) => { setSize(nextSize); setPage(0) }}
            />

            <SheetShell open={isDetailOpen} onOpenChange={setIsDetailOpen} size="xl" title="Detalles del producto" description="Informacion completa del producto y su receta asociada.">
                <div className="mt-6">{detailProductId && <ProductHubDetail productId={detailProductId} />}</div>
            </SheetShell>
            <SheetShell open={isFormOpen} onOpenChange={setIsFormOpen} size="xl" title={editingProductId ? "Editar producto" : "Nuevo producto"} description="Configura el producto. Publicarlo en el menu es una operacion independiente.">
                <div className="mt-6"><ProductForm productId={editingProductId} onSuccess={() => setIsFormOpen(false)} /></div>
            </SheetShell>
            <ConfirmDialog
                open={!!activationTarget}
                onOpenChange={(open) => {
                    if (!open) setActivationTarget(null)
                }}
                title={activationBlocked ? "Producto vinculado" : activationTarget?.isActive ? "Desactivar producto" : "Reactivar producto"}
                description={activationDescription}
                confirmText={activationBlocked ? "Entendido" : activationTarget?.isActive ? "Desactivar" : "Reactivar"}
                cancelText="Cancelar"
                variant={activationTarget?.isActive ? "destructive" : "default"}
                onConfirm={async () => {
                    if (!activationTarget) return
                    if (activationBlocked) return
                    await setProductActive.mutateAsync({ id: activationTarget.id, isActive: !activationTarget.isActive })
                }}
            />
        </div>
    )
}
