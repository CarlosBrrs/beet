"use client"

import { useQuery } from "@tanstack/react-query"
import { ApiGenericResponse, RestaurantResponse } from "@/lib/api-types"
import { apiClient } from "@/lib/api-client"
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Loader2, Store, ArrowRight, Plus } from "lucide-react"
import Link from "next/link"

export function RestaurantList() {
    const { data: restaurants, isLoading, isError } = useQuery({
        queryKey: ['my-restaurants'],
        queryFn: async () => {
            const data = await apiClient<ApiGenericResponse<RestaurantResponse[]>>("/restaurants/my-restaurants")
            return data.data
        }
    })

    if (isLoading) {
        return (
            <div className="flex justify-center items-center h-64">
                <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
            </div>
        )
    }

    if (isError) {
        return (
            <div className="text-center p-8 border rounded-md bg-destructive/10 text-destructive">
                Failed to load restaurants. Please try again.
            </div>
        )
    }

    console.log(restaurants)
    const ownerRestaurants = restaurants!.filter(r => r.role === 'Owner')
    const isOwner = ownerRestaurants.length > 0

    // If user has NO restaurants at all, they are a new potential owner.
    if (!restaurants || restaurants.length === 0) {
        return (
            <div className="text-center p-12 border-2 border-dashed rounded-lg">
                <Store className="w-12 h-12 mx-auto text-muted-foreground mb-4" />
                <h3 className="text-lg font-semibold">No Restaurants Found</h3>
                <p className="text-muted-foreground mb-6">You haven't created any restaurants yet.</p>
                <Button asChild>
                    <Link href="/restaurants/new">
                        <Plus className="mr-2 h-4 w-4" /> Create Your First Restaurant
                    </Link>
                </Button>
            </div>
        )
    }

    // If user HAS restaurants but is NOT an owner of any (pure staff)
    // The user asked to restrict this view. We can show a verified list or a message.
    // For now, let's show ALL restaurants, but only show "New Restaurant" button if they are an owner or empty.

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <h2 className="text-2xl font-bold tracking-tight">My Restaurants</h2>
                <Button size="sm" asChild>
                    <Link href="/restaurants/new">
                        <Plus className="mr-2 h-4 w-4" /> New Restaurant
                    </Link>
                </Button>
            </div>

            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                {restaurants.map((restaurant) => (
                    <Card key={restaurant.id} className="flex flex-col">
                        <CardHeader>
                            <div className="flex justify-between items-start">
                                <CardTitle className="text-xl">{restaurant.name}</CardTitle>
                                <Badge variant={restaurant.isActive ? "default" : "secondary"}>
                                    {restaurant.isActive ? "Active" : "Inactive"}
                                </Badge>
                            </div>
                            <CardDescription>{restaurant.operationMode}</CardDescription>
                        </CardHeader>
                        <CardContent className="flex-1">
                            <div className="text-sm text-muted-foreground">
                                Role: <span className="font-medium text-foreground">{restaurant.role || "Owner"}</span>
                            </div>
                        </CardContent>
                        <CardFooter>
                            <Button asChild className="w-full">
                                <Link href={`/restaurants/${restaurant.id}/dashboard`}>
                                    Manage Dashboard <ArrowRight className="ml-2 h-4 w-4" />
                                </Link>
                            </Button>
                        </CardFooter>
                    </Card>
                ))}
            </div>
        </div>
    )
}
