"use client"

import { useAuth } from "@/components/providers/auth-provider"
import { useMyPermissions } from "@/lib/hooks/use-my-permissions"
import { useMyRestaurants } from "@/lib/hooks/use-my-restaurants"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Store, Archive, ChefHat, DollarSign, User, ShieldCheck } from "lucide-react"

export default function AccountDashboardPage() {
    const { user } = useAuth()
    const { data: permissions, isLoading: permLoading, isOwner } = useMyPermissions()
    const { data: restaurants, isLoading: restLoading } = useMyRestaurants()

    const ownerView = isOwner()

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-3xl font-bold">Welcome back, {user?.firstName ?? "User"}!</h1>
                <p className="text-muted-foreground mt-1">
                    {ownerView
                        ? "Here's an overview of your business."
                        : "Here's your workspace."}
                </p>
            </div>

            {/* ══════════════════════════════════════
               OWNER-ONLY CARDS
               ══════════════════════════════════════ */}
            {ownerView && (
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Total Restaurants</CardTitle>
                            <Store className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            {restLoading
                                ? <div className="text-2xl font-bold text-muted-foreground">...</div>
                                : <div className="text-2xl font-bold">{restaurants?.length ?? 0}</div>}
                            <p className="text-xs text-muted-foreground">
                                {restaurants
                                    ? `${restaurants.filter(r => r.isActive).length} active`
                                    : "Loading..."}
                            </p>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Ingredients Catalog</CardTitle>
                            <Archive className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">—</div>
                            <p className="text-xs text-muted-foreground">Master catalog items</p>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Recipes</CardTitle>
                            <ChefHat className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold">—</div>
                            <p className="text-xs text-muted-foreground">Active recipes</p>
                        </CardContent>
                    </Card>
                    {/* <Card>
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-sm font-medium">Subscription</CardTitle>
                            <DollarSign className="h-4 w-4 text-muted-foreground" />
                        </CardHeader>
                        <CardContent>
                            <div className="text-2xl font-bold truncate text-sm mt-1">
                                {user?.subscriptionPlanId ? user.subscriptionPlanId.slice(0, 8) + "..." : "—"}
                            </div>
                            <p className="text-xs text-muted-foreground">Current plan</p>
                        </CardContent>
                    </Card> */}
                </div>
            )}

            {/* ══════════════════════════════════════
               NON-OWNER CARDS (employee view)
               ══════════════════════════════════════ */}
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
                                {permissions?.map(p => p.role).join(", ") ?? "No roles assigned"}
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
                                {permissions?.filter(p => p.restaurantId !== null).length ?? 0}
                            </div>
                            <p className="text-xs text-muted-foreground">Restaurants you can access</p>
                        </CardContent>
                    </Card>
                </div>
            )}

            {/* ══════════════════════════════════════
               SHARED CARDS (both owner & non-owner)
               ══════════════════════════════════════ */}
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

            {/* ═══════════════════════════════════════════════════════════════
                TEMPORARY DEBUG SECTION — DELETE LATER
                ═══════════════════════════════════════════════════════════════ */}
            <div className="space-y-4 border-t-2 border-dashed border-yellow-500 pt-6 mt-8">
                <h2 className="text-lg font-bold text-yellow-600">
                    🔧 DEBUG — isOwner: {ownerView ? "YES" : "NO"}
                </h2>

                <Card className="border-yellow-300 bg-yellow-50 dark:bg-yellow-950">
                    <CardHeader>
                        <CardTitle className="text-sm font-mono">GET /auth/my-permissions</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {permLoading && <p className="text-sm text-muted-foreground">Loading...</p>}
                        {permissions && (
                            <pre className="text-xs overflow-auto max-h-60 bg-black/5 dark:bg-white/5 p-3 rounded">
                                {JSON.stringify(permissions, null, 2)}
                            </pre>
                        )}
                    </CardContent>
                </Card>

                {ownerView && (
                    <Card className="border-yellow-300 bg-yellow-50 dark:bg-yellow-950">
                        <CardHeader>
                            <CardTitle className="text-sm font-mono">GET /restaurants/my-restaurants</CardTitle>
                        </CardHeader>
                        <CardContent>
                            {restLoading && <p className="text-sm text-muted-foreground">Loading...</p>}
                            {restaurants && (
                                <pre className="text-xs overflow-auto max-h-60 bg-black/5 dark:bg-white/5 p-3 rounded">
                                    {JSON.stringify(restaurants, null, 2)}
                                </pre>
                            )}
                        </CardContent>
                    </Card>
                )}

                <Card className="border-yellow-300 bg-yellow-50 dark:bg-yellow-950">
                    <CardHeader>
                        <CardTitle className="text-sm font-mono">Auth Context (user)</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <pre className="text-xs overflow-auto max-h-60 bg-black/5 dark:bg-white/5 p-3 rounded">
                            {JSON.stringify(user, null, 2)}
                        </pre>
                    </CardContent>
                </Card>
            </div>
        </div>
    )
}
