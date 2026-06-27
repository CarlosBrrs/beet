"use client"

import { ChevronLeft, ChevronRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

interface ListPaginationProps {
    totalElements: number
    totalPages: number
    page: number
    size: number
    label: string
    onPageChange: (page: number) => void
    onSizeChange: (size: number) => void
}

export function ListPagination({
    totalElements,
    totalPages,
    page,
    size,
    label,
    onPageChange,
    onSizeChange,
}: ListPaginationProps) {
    return (
        <div className="flex flex-col gap-3 border-t pt-4 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
            <span>{totalElements} {label}</span>
            <div className="flex items-center gap-2">
                <span>Filas</span>
                <Select value={String(size)} onValueChange={(value) => onSizeChange(Number(value))}>
                    <SelectTrigger className="w-[72px]">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        {[10, 20, 50].map((option) => (
                            <SelectItem key={option} value={String(option)}>{option}</SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <span>Pagina {totalPages === 0 ? 0 : page + 1} de {totalPages}</span>
                <Button
                    variant="outline"
                    size="icon"
                    title="Pagina anterior"
                    disabled={page === 0}
                    onClick={() => onPageChange(Math.max(page - 1, 0))}
                >
                    <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                    variant="outline"
                    size="icon"
                    title="Pagina siguiente"
                    disabled={page + 1 >= totalPages}
                    onClick={() => onPageChange(page + 1)}
                >
                    <ChevronRight className="h-4 w-4" />
                </Button>
            </div>
        </div>
    )
}
