"use client"

import { useProduct } from "@/lib/hooks/use-submenu-nodes"
import { Skeleton } from "@/components/ui/skeleton"
import { formatCurrency } from "@/lib/formatters"
import { Badge } from "@/components/ui/badge"

interface ProductDetailProps {
    menuId: string
    submenuId: string
    productId: string
}

export function ProductDetail({ menuId, submenuId, productId }: ProductDetailProps) {
    const { data: product, isLoading, isError } = useProduct(menuId, submenuId, productId)

    if (isLoading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-4 w-[250px]" />
                <Skeleton className="h-4 w-[200px]" />
                <Skeleton className="h-4 w-[150px]" />
            </div>
        )
    }

    if (isError || !product) {
        return <div className="text-destructive">Error al cargar detalles del producto.</div>
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between">
                <div>
                    <h2 className="text-xl font-bold">{product.name}</h2>
                    {product.description && <p className="text-muted-foreground">{product.description}</p>}
                </div>
                {product.isInventoryTracked && (
                    <Badge variant="secondary" className="bg-amber-100 text-amber-800 hover:bg-amber-100">
                        Rastreado en Bodega
                    </Badge>
                )}
            </div>

            <div className="grid grid-cols-2 gap-4">
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Precio de Venta</h4>
                    <p className="text-lg font-semibold">{formatCurrency(product.salePrice || 0)}</p>
                </div>
                <div>
                    <h4 className="text-sm font-medium text-muted-foreground">Porciones del lote</h4>
                    <p className="text-lg">{product.sellableUnitsPerBatch ?? 1}</p>
                    {product.portionSize !== null && (
                        <p className="text-xs text-muted-foreground">
                            {product.portionSize} {product.portionUnitAbbreviation} por porción
                        </p>
                    )}
                </div>
            </div>

            {/* Recipe Lines */}
            <div className="pt-4 border-t">
                <h4 className="font-semibold mb-4">Receta / Composición</h4>
                {product.recipeLines && product.recipeLines.length > 0 ? (
                    <div className="rounded-md border">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b bg-muted/50 text-muted-foreground">
                                    <th className="h-10 px-4 text-left font-medium">Elemento</th>
                                    <th className="h-10 px-4 text-left font-medium">Tipo</th>
                                    <th className="h-10 px-4 text-left font-medium">Cantidad</th>
                                </tr>
                            </thead>
                            <tbody>
                                {product.recipeLines.map((line: import("@/lib/api-types").RecipeLineResponse) => (
                                    <tr key={line.id} className="border-b last:border-0 hover:bg-muted/50 transition-colors">
                                        <td className="p-4 align-middle">
                                            {line.sourceName ?? "Elemento no disponible"}
                                        </td>
                                        <td className="p-4 align-middle capitalize text-muted-foreground">
                                            {line.source === "INGREDIENT" ? "Insumo" : "Preparación"}
                                        </td>
                                        <td className="p-4 align-middle">
                                            {line.quantity} {line.unitAbbreviation}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <p className="text-sm text-muted-foreground italic">Este producto no tiene una receta definida (es un ítem plano).</p>
                )}
            </div>

            <div className="pt-4 border-t">
                <h4 className="text-sm font-medium text-muted-foreground mb-2">Información del Sistema</h4>
                <div className="text-xs text-muted-foreground space-y-1 font-mono">
                    <p>ID: {product.id}</p>
                </div>
            </div>
        </div>
    )
}
