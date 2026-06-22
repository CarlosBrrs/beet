"use client"

import { useAuth } from "@/components/providers/auth-provider"
import { useMyPermissions } from "@/lib/hooks/use-my-permissions"
import { useMyRestaurants } from "@/lib/hooks/use-my-restaurants"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Archive, ChefHat, ShieldCheck, Store, User } from "lucide-react"

export default function AccountDashboardPage() {
    const { user } = useAuth()
    const { data: permissions, isLoading: permLoading, isOwner } = useMyPermissions()
    const { data: restaurants, isLoading: restLoading } = useMyRestaurants()

    const ownerView = isOwner()
    const activeRestaurants = restaurants?.filter((restaurant) => restaurant.isActive).length ?? 0

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-3xl font-bold">Welcome back, {user?.firstName ?? "User"}!</h1>
                <p className="mt-1 text-muted-foreground">
                    {ownerView ? "Here's an overview of your business." : "Here's your workspace."}
                </p>
            </div>

            {ownerView && (
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Total Restaurants</CardTitle>
                            <Store className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">{restLoading ? "..." : restaurants?.length ?? 0}</div>
                            <p className="text-xs text-muted-foreground">{restLoading ? "Loading..." : `${activeRestaurants} active`}</p>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Ingredients Catalog</CardTitle>
                            <Archive className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">-</div>
                            <p className="text-xs text-muted-foreground">Master catalog items</p>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Recipes</CardTitle>
                            <ChefHat className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">-</div>
                            <p className="text-xs text-muted-foreground">Active recipes</p>
                        </CardContent>
                    </Card>
                </div>
            )}

            {!ownerView && !permLoading && (
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">My Roles</CardTitle>
                            <ShieldCheck className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">{permissions?.length ?? 0}</div>
                            <p className="text-xs text-muted-foreground">
                                {permissions?.map((permission) => permission.role).join(", ") ?? "No roles assigned"}
                            </p>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Assigned Restaurants</CardTitle>
                            <Store className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">
                                {permissions?.filter((permission) => permission.restaurantId !== null).length ?? 0}
                            </div>
                            <p className="text-xs text-muted-foreground">Restaurants you can access</p>
                        </CardContent>
                    </Card>
                </div>
            )}

            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                <Card>
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                        <CardTitle className="text-sm font-medium">Profile</CardTitle>
                        <User className="h-4 w-4 text-muted-foreground" />
                    </CardHeader>
                    <CardContent>
                        <div className="text-sm font-medium">{user?.firstName} {user?.firstLastname}</div>
                        <p className="text-xs text-muted-foreground">{user?.email}</p>
                    </CardContent>
                </Card>
            </div>
        </div>
    )
}