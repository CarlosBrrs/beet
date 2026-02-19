"use client"

import * as React from "react"
import { Check, ChevronsUpDown, PlusCircle, Store } from "lucide-react"
import { useRouter, useParams } from "next/navigation"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
    CommandSeparator,
} from "@/components/ui/command"
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/components/ui/popover"
import { useQuery } from "@tanstack/react-query"
import { ApiGenericResponse, RestaurantResponse } from "@/lib/api-types"
import { apiClient } from "@/lib/api-client"

export function RestaurantSelector({ className }: { className?: string }) {
    const router = useRouter()
    const params = useParams()
    const currentRestaurantId = params?.id as string
    const [open, setOpen] = React.useState(false)

    // Fetch My Restaurants
    const { data: restaurants = [], isLoading } = useQuery({
        queryKey: ['my-restaurants'],
        queryFn: async () => {
            const data = await apiClient<ApiGenericResponse<RestaurantResponse[]>>("/restaurants/my-restaurants")
            return data.data
        }
    })

    const selectedRestaurant = restaurants.find(r => r.id === currentRestaurantId)

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <Button
                    variant="outline"
                    role="combobox"
                    aria-expanded={open}
                    aria-label="Select a restaurant"
                    className={cn("w-[200px] justify-between bg-slate-200 border-slate-300 hover:bg-slate-300", className)}
                >
                    <Store className="mr-2 h-4 w-4" />
                    {selectedRestaurant ? selectedRestaurant.name : "Select Restaurant..."}
                    <ChevronsUpDown className="ml-auto h-4 w-4 opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-[200px] p-0">
                <Command>
                    <CommandList>
                        <CommandInput placeholder="Search restaurant..." />
                        <CommandEmpty>No restaurant found.</CommandEmpty>
                        <CommandGroup heading="My Restaurants">
                            {restaurants.map((restaurant) => (
                                <CommandItem
                                    key={restaurant.id}
                                    onSelect={() => {
                                        router.push(`/restaurants/${restaurant.id}/dashboard`)
                                        setOpen(false)
                                    }}
                                    className="text-sm"
                                >
                                    <Store className="mr-2 h-4 w-4" />
                                    {restaurant.name}
                                    <Check
                                        className={cn(
                                            "ml-auto h-4 w-4",
                                            currentRestaurantId === restaurant.id
                                                ? "opacity-100"
                                                : "opacity-0"
                                        )}
                                    />
                                </CommandItem>
                            ))}
                        </CommandGroup>
                    </CommandList>
                    <CommandSeparator />
                    <CommandList>
                        <CommandGroup>
                            <CommandItem
                                onSelect={() => {
                                    setOpen(false)
                                    router.push("/restaurants/new") // Or modal
                                }}
                            >
                                <PlusCircle className="mr-2 h-5 w-5" />
                                Create Restaurant
                            </CommandItem>
                        </CommandGroup>
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    )
}
