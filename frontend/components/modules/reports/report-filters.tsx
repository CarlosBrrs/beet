"use client"

import { CalendarRange } from "lucide-react"

import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { RestaurantResponse } from "@/lib/api-types"
import { ReportGrouping } from "@/lib/hooks/use-reports"

interface ReportFiltersProps {
    dateFrom: string
    dateTo: string
    grouping: ReportGrouping
    restaurants?: RestaurantResponse[]
    selectedRestaurantIds: string[]
    onDateFromChange: (value: string) => void
    onDateToChange: (value: string) => void
    onGroupingChange: (value: ReportGrouping) => void
    onRestaurantSelectionChange: (ids: string[]) => void
}

export function ReportFilters({
    dateFrom,
    dateTo,
    grouping,
    restaurants,
    selectedRestaurantIds,
    onDateFromChange,
    onDateToChange,
    onGroupingChange,
    onRestaurantSelectionChange,
}: ReportFiltersProps) {
    const allSelected = selectedRestaurantIds.length === 0

    return (
        <section className="border">
            <div className="flex flex-col gap-4 p-4">
                <div className="flex flex-wrap items-end gap-3">
                    <div className="min-w-[160px] flex-1">
                        <label className="mb-1.5 block text-xs font-medium">Desde</label>
                        <Input type="date" value={dateFrom} onChange={(event) => onDateFromChange(event.target.value)} />
                    </div>
                    <div className="min-w-[160px] flex-1">
                        <label className="mb-1.5 block text-xs font-medium">Hasta</label>
                        <Input type="date" value={dateTo} onChange={(event) => onDateToChange(event.target.value)} />
                    </div>
                    <div className="min-w-[160px]">
                        <label className="mb-1.5 block text-xs font-medium">Agrupacion</label>
                        <Select value={grouping} onValueChange={(value) => onGroupingChange(value as ReportGrouping)}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                                <SelectItem value="DAY">Dia</SelectItem>
                                <SelectItem value="WEEK">Semana</SelectItem>
                                <SelectItem value="MONTH">Mes</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>
                    <div className="flex h-9 items-center gap-2 px-2 text-xs text-muted-foreground">
                        <CalendarRange className="h-4 w-4" />
                        Maximo 366 dias
                    </div>
                </div>

                {restaurants && restaurants.length > 0 && (
                    <div className="border-t pt-3">
                        <p className="mb-2 text-xs font-medium">Restaurantes</p>
                        <div className="flex flex-wrap gap-x-5 gap-y-2">
                            <label className="flex items-center gap-2 text-xs">
                                <Checkbox
                                    checked={allSelected}
                                    onCheckedChange={() => onRestaurantSelectionChange([])}
                                />
                                Todos
                            </label>
                            {restaurants.map((restaurant) => {
                                const checked = selectedRestaurantIds.includes(restaurant.id)
                                return (
                                    <label key={restaurant.id} className="flex items-center gap-2 text-xs">
                                        <Checkbox
                                            checked={checked}
                                            onCheckedChange={(next) => {
                                                const ids = next
                                                    ? [...selectedRestaurantIds, restaurant.id]
                                                    : selectedRestaurantIds.filter((id) => id !== restaurant.id)
                                                onRestaurantSelectionChange(ids)
                                            }}
                                        />
                                        {restaurant.name}
                                    </label>
                                )
                            })}
                        </div>
                    </div>
                )}
            </div>
        </section>
    )
}
