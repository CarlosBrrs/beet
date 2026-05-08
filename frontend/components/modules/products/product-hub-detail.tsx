"use client"

import { useQuery } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import { ApiGenericResponse, ItemResponse } from "@/lib/api-types"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { Skeleton } from "@/components/ui/skeleton"
import { Badge } from "@/components/ui/badge"
import { formatCurrency } from "@/lib/formatters"

// ── Hook ─────────────────────────────────────────────────────────────────────

function useProductById(productId: string | null) {
    const { restaurantId } = useRestaurantContext()

    return useQuery({
        queryKey: ["items", restaurantId, productId],
        queryFn: async () => {
            const data = await apiClient<ApiGenericResponse<ItemResponse>>(
                `/restaurants/${restaurantId}/items/${productId}`,
                { method: "GET" }
            )
            return data.data
        },
        enabled: !!restaurantId && !!productId,
    })
}

// ── Component ─────────────────────────────────────────────────────────────────

interface ProductHubDetailProps {
    productId: string
}

export function ProductHubDetail({ productId }: ProductHubDetailProps) {
    const { data: product, isLoading, isError } = useProductById(productId)

    if (isLoading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-6 w-[260px]" />
                <Skeleton className="h-4 w-[180px]" />
                <Skeleton className="h-4 w-[220px]" />
            </div>
        )
    }

    if (isError || !product) {
        return <div className="text-destructive text-sm">Error al cargar los detalles del producto.</div>
    }

    const margin =
        product.salePrice && product.theoreticalCost && product.salePrice > 0
            ? (((product.salePrice - product.theoreticalCost) / product.salePrice) * 100).toFixed(1)
            : null

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h2 className="text-xl font-bold">{product.name}</h2>
                    {product.description && (
                        <p className="text-muted-foreground text-sm mt-1">{product.description}</p>
                    )}
                </div>
                {product.isInventoryTracked && (
                    <Badge variant="secondary" className="bg-amber-100 text-amber-800 hover:bg-amber-100 shrink-0">
                        Rastreado en Bodega
                    </Badge>
                )}
            </div>

            {/* Key Metrics */}
            <div className="grid grid-cols-3 gap-4">
                <div className="rounded-lg border bg-muted/20 p-3">
                    <p className="text-xs text-muted-foreground mb-1">Precio de Venta</p>
                    <p className="text-lg font-semibold">{formatCurrency(product.salePrice || 0)}</p>
                </div>
                <div className="rounded-lg border bg-muted/20 p-3">
                    <p className="text-xs text-muted-foreground mb-1">Costo Teórico</p>
                    <p className="text-lg font-semibold">
                        {product.theoreticalCost ? formatCurrency(product.theoreticalCost) : "—"}
                    </p>
                </div>
                <div className="rounded-lg border bg-muted/20 p-3">
                    <p className="text-xs text-muted-foreground mb-1">Margen</p>
                    <p className={`text-lg font-semibold ${margin && parseFloat(margin) < 30 ? "text-destructive" : "text-green-600"}`}>
                        {margin ? `${margin}%` : "—"}
                    </p>
                </div>
            </div>

            {/* Recipe Lines */}
            <div className="pt-2 border-t">
                <h4 className="font-semibold mb-3 text-sm">Receta / Composición</h4>
                {product.recipeLines && product.recipeLines.length > 0 ? (
                    <div className="rounded-md border overflow-hidden">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="border-b bg-muted/50 text-muted-foreground">
                                    <th className="h-9 px-4 text-left font-medium">Elemento</th>
                                    <th className="h-9 px-4 text-left font-medium">Tipo</th>
                                    <th className="h-9 px-4 text-right font-medium">Cantidad</th>
                                </tr>
                            </thead>
                            <tbody>
                                {product.recipeLines.map((line) => (
                                    <tr key={line.id} className="border-b last:border-0 hover:bg-muted/30 transition-colors">
                                        <td className="p-3 align-middle font-mono text-xs">
                                            {line.source === "INGREDIENT" ? line.masterIngredientId : line.childItemId}
                                        </td>
                                        <td className="p-3 align-middle">
                                            <Badge variant="outline" className="text-xs font-normal">
                                                {line.source === "INGREDIENT" ? "Insumo" : "Preparación"}
                                            </Badge>
                                        </td>
                                        <td className="p-3 align-middle text-right">{line.quantity}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <p className="text-sm text-muted-foreground italic">
                        Producto plano — sin receta asociada.
                    </p>
                )}
            </div>

            {/* System Info */}
            <div className="pt-2 border-t">
                <p className="text-xs text-muted-foreground font-mono">ID: {product.id}</p>
            </div>
        </div>
    )
}
