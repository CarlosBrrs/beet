"use client"

import { useSubmenuNodes, useDeleteSubmenuNode } from "@/lib/hooks/use-submenu-nodes"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Loader2, Plus, GripVertical, Settings2, Trash2, Box, Layers, Eye, Pencil } from "lucide-react"
import { formatNumber } from "@/lib/formatters"
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from "@/components/ui/alert-dialog"

interface SubmenuNodeListProps {
    menuId: string
    submenuId: string
    onAddProduct: () => void
    onAddTemplate: () => void
    onViewNode: (nodeId: string, type: "PRODUCT" | "TEMPLATE") => void
    onEditNode: (nodeId: string, type: "PRODUCT" | "TEMPLATE") => void
}

export function SubmenuNodeList({ menuId, submenuId, onAddProduct, onAddTemplate, onViewNode, onEditNode }: SubmenuNodeListProps) {
    const { data: nodes, isLoading } = useSubmenuNodes(menuId, submenuId)
    const deleteNode = useDeleteSubmenuNode(menuId, submenuId)

    if (isLoading) {
        return (
            <div className="flex justify-center p-8">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
        )
    }

    const isEmpty = !nodes || nodes.length === 0

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h3 className="text-sm font-medium text-muted-foreground uppercase tracking-wider">
                    Elementos ({nodes?.length || 0})
                </h3>
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="sm">
                            <Plus className="h-4 w-4 mr-2" />
                            Agregar
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={onAddProduct}>
                            <Box className="mr-2 h-4 w-4 text-emerald-500" />
                            Producto
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={onAddTemplate}>
                            <Layers className="mr-2 h-4 w-4 text-indigo-500" />
                            Plantilla / Combo
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {isEmpty ? (
                <Card className="border-dashed">
                    <CardContent className="flex flex-col items-center justify-center p-8 text-center text-muted-foreground">
                        <Box className="h-8 w-8 mb-4 opacity-50" />
                        <p>No hay productos ni plantillas en este submenú.</p>
                        <p className="text-sm mt-1">Haz clic en Agregar para comenzar.</p>
                    </CardContent>
                </Card>
            ) : (
                <div className="space-y-2">
                    {nodes.map((node) => {
                        const isProduct = node.nodeType === "PRODUCT"
                        const name = isProduct ? node.item?.name : node.template?.name
                        const price = isProduct ? node.item?.salePrice : node.template?.basePrice

                        return (
                            <Card key={node.id} className="overflow-hidden group">
                                <CardContent className="p-0 flex items-stretch">
                                    <div className="w-8 flex items-center justify-center bg-muted/40 text-muted-foreground border-r cursor-move hover:bg-muted/60 transition-colors">
                                        <GripVertical className="h-4 w-4 opacity-50" />
                                    </div>
                                    <div className="flex-1 p-4 flex items-center justify-between">
                                        <div className="flex items-center gap-3">
                                            {isProduct ? (
                                                <div className="h-8 w-8 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center">
                                                    <Box className="h-4 w-4" />
                                                </div>
                                            ) : (
                                                <div className="h-8 w-8 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center">
                                                    <Layers className="h-4 w-4" />
                                                </div>
                                            )}
                                            <div>
                                                <h4 className="font-medium text-sm">{name}</h4>
                                                <p className="text-xs text-muted-foreground flex items-center gap-2">
                                                    <span className="capitalize">{node.nodeType.toLowerCase()}</span>
                                                    {isProduct && node.item?.isInventoryTracked && (
                                                        <>
                                                            <span>•</span>
                                                            <span className="text-amber-600">Tracked</span>
                                                        </>
                                                    )}
                                                </p>
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-4">
                                            <div className="text-right">
                                                <p className="font-mono text-sm font-medium">
                                                    ${formatNumber(price || 0)}
                                                </p>
                                            </div>
                                            <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className="h-8 w-8"
                                                    onClick={() => onViewNode(node.id, node.nodeType)}
                                                >
                                                    <Eye className="h-4 w-4" />
                                                </Button>
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className="h-8 w-8"
                                                    onClick={() => onEditNode(node.id, node.nodeType)}
                                                >
                                                    <Pencil className="h-4 w-4" />
                                                </Button>

                                                <AlertDialog>
                                                    <AlertDialogTrigger asChild>
                                                        <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive hover:text-destructive">
                                                            <Trash2 className="h-4 w-4" />
                                                        </Button>
                                                    </AlertDialogTrigger>
                                                    <AlertDialogContent>
                                                        <AlertDialogHeader>
                                                            <AlertDialogTitle>Remover Elemento</AlertDialogTitle>
                                                            <AlertDialogDescription>
                                                                ¿Estás seguro que deseas remover <strong>{name}</strong> de este submenú?
                                                                El elemento ya no será visible aquí pero no será borrado del sistema general.
                                                            </AlertDialogDescription>
                                                        </AlertDialogHeader>
                                                        <AlertDialogFooter>
                                                            <AlertDialogCancel>Cancelar</AlertDialogCancel>
                                                            <AlertDialogAction
                                                                className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                                                onClick={() => deleteNode.mutate(node.id)}
                                                            >
                                                                Remover
                                                            </AlertDialogAction>
                                                        </AlertDialogFooter>
                                                    </AlertDialogContent>
                                                </AlertDialog>
                                            </div>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        )
                    })}
                </div>
            )}
        </div>
    )
}
