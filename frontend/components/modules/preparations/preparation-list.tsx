"use client"

import { useState } from "react";
import { usePreparations, useDeleteItem } from "@/lib/hooks/use-items";
import { useUnits } from "@/lib/hooks/use-units";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
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
} from "@/components/ui/alert-dialog";
import { Loader2, Plus, Trash2, ChefHat, FlaskConical } from "lucide-react";
import { ItemResponse } from "@/lib/api-types";
import { formatCurrency } from "@/lib/formatters";

interface PreparationListProps {
    onNew: () => void;
    onEdit: (item: ItemResponse) => void;
}

export function PreparationList({ onNew, onEdit }: PreparationListProps) {
    const { data: preparations, isLoading, isError } = usePreparations();
    const { data: units = [] } = useUnits();
    const deleteItem = useDeleteItem();

    if (isLoading) {
        return (
            <div className="flex py-12 items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
        );
    }

    if (isError) {
        return (
            <div className="text-center py-12 text-destructive">
                Error al cargar las preparaciones. Por favor intenta de nuevo.
            </div>
        );
    }

    if (!preparations || preparations.length === 0) {
        return (
            <div className="flex flex-col items-center justify-center py-20 border border-dashed rounded-xl bg-muted/20 text-center gap-4">
                <FlaskConical className="h-12 w-12 text-muted-foreground/40" />
                <div>
                    <p className="font-medium text-foreground">Sin preparaciones</p>
                    <p className="text-sm text-muted-foreground mt-1">
                        Crea tu primera preparación interna (salsas, mezclas, etc.).
                    </p>
                </div>
                <Button onClick={onNew} size="sm">
                    <Plus className="mr-2 h-4 w-4" /> Nueva Preparación
                </Button>
            </div>
        );
    }

    return (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {preparations.map((item) => (
                <PreparationCard
                    key={item.id}
                    item={item}
                    units={units}
                    onEdit={() => onEdit(item)}
                    onDelete={() => deleteItem.mutate(item.id)}
                    isDeleting={deleteItem.isPending}
                />
            ))}
        </div>
    );
}

function PreparationCard({
    item,
    units,
    onEdit,
    onDelete,
    isDeleting,
}: {
    item: ItemResponse;
    units: { id: string; abbreviation: string; isBase: boolean; type: string }[];
    onEdit: () => void;
    onDelete: () => void;
    isDeleting: boolean;
}) {
    const yieldUnit = units.find(u => u.id === item.yieldUnitId);
    const baseUnitCost = yieldUnit
        ? units.find(u => u.isBase && yieldUnit.type !== undefined /* just to get from same category generically, but currently we just display base unit */)
        // fallback
        : null;
    return (
        <Card
            className="relative group flex flex-col h-full bg-card cursor-pointer hover:shadow-md transition-shadow"
            onClick={onEdit}
        >
            <CardHeader className="pb-2 flex-none">
                <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2 min-w-0">
                        <ChefHat className="h-5 w-5 text-primary shrink-0" />
                        <CardTitle className="text-base truncate">{item.name}</CardTitle>
                    </div>
                    <AlertDialog>
                        <AlertDialogTrigger asChild onClick={(e) => e.stopPropagation()}>
                            <Button
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7 shrink-0 opacity-0 group-hover:opacity-100 text-destructive hover:text-destructive hover:bg-destructive/10 transition-opacity"
                                disabled={isDeleting}
                            >
                                {isDeleting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                            </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent onClick={(e) => e.stopPropagation()}>
                            <AlertDialogHeader>
                                <AlertDialogTitle>¿Eliminar preparación?</AlertDialogTitle>
                                <AlertDialogDescription>
                                    Se eliminará <strong>{item.name}</strong> y todas sus líneas de receta. Esta acción no se puede deshacer.
                                </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                                <AlertDialogCancel>Cancelar</AlertDialogCancel>
                                <AlertDialogAction
                                    onClick={onDelete}
                                    className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                >
                                    Eliminar
                                </AlertDialogAction>
                            </AlertDialogFooter>
                        </AlertDialogContent>
                    </AlertDialog>
                </div>
                {item.description && (
                    <CardDescription className="line-clamp-2 text-xs mt-1">
                        {item.description}
                    </CardDescription>
                )}
            </CardHeader>
            <CardContent className="flex-1 flex flex-col gap-2 pt-0">
                <div className="flex flex-wrap gap-2">
                    {item.yieldQty && (
                        <Badge variant="outline" className="text-xs">
                            Rendimiento: {item.yieldQty} {yieldUnit?.abbreviation}
                        </Badge>
                    )}
                    <Badge variant="secondary" className="text-xs">
                        {item.recipeLines.length} ingredientes
                    </Badge>
                </div>
                {item.theoreticalCost != null && (
                    <p className="text-sm font-mono text-muted-foreground mt-auto">
                        Costo teórico:{" "}
                        <span className="font-semibold text-foreground">
                            {formatCurrency(item.theoreticalCost)}
                            <span className="text-xs font-normal text-muted-foreground ml-1">
                                / unidad base
                            </span>
                        </span>
                    </p>
                )}
            </CardContent>
        </Card>
    );
}
