"use client"

import { useState } from "react"
import { useProducts } from "@/lib/hooks/use-items"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { SheetShell } from "@/components/shared/sheet-shell"
import { ProductHubDetail } from "@/components/modules/products/product-hub-detail"
import { Eye, Search, PackageSearch, Info } from "lucide-react"
import { formatCurrency } from "@/lib/formatters"
import { useDebounce } from "@/lib/hooks/use-debounce"

export default function ProductsPage() {
    const [search, setSearch] = useState("")
    const [selectedProductId, setSelectedProductId] = useState<string | null>(null)
    const [isDetailOpen, setIsDetailOpen] = useState(false)

    const debouncedSearch = useDebounce(search, 300)
    const { data, isLoading } = useProducts({ search: debouncedSearch, size: 50 })
    const products = data?.content ?? []

    const handleViewProduct = (id: string) => {
        setSelectedProductId(id)
        setIsDetailOpen(true)
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-3xl font-bold tracking-tight">Productos</h1>
                <p className="text-muted-foreground mt-1">
                    Vista general de todos los productos vendibles del restaurante.
                </p>
            </div>

            <div className="flex gap-3 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950">
                <Info className="mt-0.5 h-4 w-4 shrink-0" />
                <div className="space-y-1">
                    <p className="font-medium">Pendiente tecnico: respuesta enriquecida de productos</p>
                    <p>
                        Agregar unidades legibles para rendimiento y lineas de receta, mas costo unitario usado,
                        costo total por linea y aporte por unidad vendible. Calcularlo en backend para lectura;
                        guardar snapshots solo cuando existan ordenes.
                    </p>
                    <p className="font-medium mt-3">Pendiente tecnico: impuestos en ordenes</p>
                    <p>
                        TODO: Los impuestos de ordenes se derivaran de restaurant_taxes o del
                        defaultTaxPercentage si no hay impuestos configurados. En MVP no hay
                        desglose por item; a futuro se agregara order_item_taxes con snapshots por
                        impuesto para mantener historial estable.
                    </p>
                </div>
            </div>

            {/* Search */}
            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                    placeholder="Buscar producto..."
                    className="pl-9"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                />
            </div>

            {/* Table */}
            {isLoading ? (
                <div className="space-y-2">
                    {[1, 2, 3, 4].map((i) => (
                        <Skeleton key={i} className="h-14 w-full rounded-lg" />
                    ))}
                </div>
            ) : products.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-20 border border-dashed rounded-xl bg-muted/20 text-center gap-3">
                    <PackageSearch className="h-10 w-10 text-muted-foreground/40" />
                    <div>
                        <p className="font-medium">
                            {search ? "Sin resultados para tu búsqueda" : "No hay productos registrados"}
                        </p>
                        <p className="text-sm text-muted-foreground mt-1">
                            Los productos se crean desde la sección de Menús → Submenú.
                        </p>
                    </div>
                </div>
            ) : (
                <div className="rounded-xl border overflow-hidden">
                    <table className="w-full text-sm">
                        <thead>
                            <tr className="border-b bg-muted/50 text-muted-foreground">
                                <th className="h-11 px-4 text-left font-medium">Nombre</th>
                                <th className="h-11 px-4 text-right font-medium">Precio Venta</th>
                                <th className="h-11 px-4 text-right font-medium">Costo Teórico</th>
                                <th className="h-11 px-4 text-right font-medium">Margen</th>
                                <th className="h-11 px-4 text-center font-medium">Estado</th>
                                <th className="h-11 px-4 text-center font-medium">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            {products.map((product) => {
                                const margin =
                                    product.salePrice && product.theoreticalCost && product.salePrice > 0
                                        ? (((product.salePrice - product.theoreticalCost) / product.salePrice) * 100).toFixed(1)
                                        : null

                                return (
                                    <tr
                                        key={product.id}
                                        className="border-b last:border-0 hover:bg-muted/30 transition-colors"
                                    >
                                        <td className="p-4 font-medium">
                                            {product.name}
                                            {product.description && (
                                                <p className="text-xs text-muted-foreground font-normal mt-0.5 truncate max-w-[240px]">
                                                    {product.description}
                                                </p>
                                            )}
                                        </td>
                                        <td className="p-4 text-right font-mono">
                                            {formatCurrency(product.salePrice || 0)}
                                        </td>
                                        <td className="p-4 text-right font-mono text-muted-foreground">
                                            {product.theoreticalCost ? formatCurrency(product.theoreticalCost) : "—"}
                                        </td>
                                        <td className="p-4 text-right">
                                            {margin ? (
                                                <span className={parseFloat(margin) < 30 ? "text-destructive font-medium" : "text-green-600 font-medium"}>
                                                    {margin}%
                                                </span>
                                            ) : "—"}
                                        </td>
                                        <td className="p-4 text-center">
                                            {product.isInventoryTracked ? (
                                                <Badge variant="secondary" className="bg-amber-100 text-amber-800 hover:bg-amber-100 text-xs">
                                                    Rastreado
                                                </Badge>
                                            ) : (
                                                <Badge variant="outline" className="text-xs text-muted-foreground">
                                                    Plano
                                                </Badge>
                                            )}
                                        </td>
                                        <td className="p-4 text-center">
                                            <Button
                                                variant="ghost"
                                                size="icon"
                                                className="h-8 w-8"
                                                onClick={() => handleViewProduct(product.id)}
                                            >
                                                <Eye className="h-4 w-4" />
                                            </Button>
                                        </td>
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Detail Sheet */}
            <SheetShell
                open={isDetailOpen}
                onOpenChange={setIsDetailOpen}
                size="xl"
                title="Detalles del Producto"
                description="Información completa del producto y su receta asociada."
            >
                <div className="mt-6">
                    {selectedProductId && (
                        <ProductHubDetail productId={selectedProductId} />
                    )}
                </div>
            </SheetShell>
        </div>
    )
}
