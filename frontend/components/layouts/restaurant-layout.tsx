"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import { LayoutDashboard, Menu, ShoppingCart, Users, Settings, ArrowLeft, LogOut } from "lucide-react"
import { RestaurantProvider, useRestaurantContext } from "@/components/providers/restaurant-provider"
import { RestaurantSelector } from "@/components/modules/restaurants/restaurant-selector"
import { ScrollArea } from "@/components/ui/scroll-area"
import { SidebarNav } from "./sidebar-nav"
import { useAuth } from "@/components/providers/auth-provider"
import { Button } from "@/components/ui/button"

interface RestaurantLayoutProps {
    children: React.ReactNode
    params: { id: string }
}

export function RestaurantLayout({ children, params }: RestaurantLayoutProps) {
    const { id } = params

    return (
        <RestaurantProvider restaurantId={id}>
            <RestaurantLayoutInner>
                {children}
            </RestaurantLayoutInner>
        </RestaurantProvider>
    )
}

function RestaurantLayoutInner({ children }: { children: React.ReactNode }) {
    const { user, logout } = useAuth()

    return (
        <div className="flex h-screen overflow-hidden bg-background">

            {/* Sidebar */}
            <aside className="w-64 border-r flex flex-col h-full bg-muted/10">
                <div className="flex items-center h-16 px-6 border-b shrink-0 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
                    <h1 className="text-xl font-bold bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
                        Beet
                    </h1>
                </div>
                {/* Restaurant Selector */}
                <div className="px-4 py-3 border-b shrink-0">
                    <RestaurantSelector className="w-full" />
                </div>
                {/* 
                    ScrollArea needs a constrained height. 
                    Remaining space after header + selector.
                */}
                <div className="flex-1 overflow-hidden">
                    <ScrollArea className="h-full">
                        <div className="p-4">
                            <SidebarNav />
                        </div>
                    </ScrollArea>
                </div>

                {/* Logout */}
                <div className="px-4 py-3 border-t shrink-0">
                    <div className="flex items-center justify-between">
                        <span className="text-sm text-muted-foreground truncate">
                            {user?.firstName ?? "User"}
                        </span>
                        <Button variant="ghost" size="icon" onClick={logout} title="Logout">
                            <LogOut className="h-4 w-4" />
                        </Button>
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <main className="flex-1 overflow-y-auto px-8 pt-6">
                {children}
            </main>
        </div>
    )
}

function ContextNavLink({ href, children, icon }: { href: string, children: React.ReactNode, icon: React.ReactNode }) {
    const pathname = usePathname()
    // Simple active check: strictly equal or starts with (for sub-routes)
    const isActive = pathname === href || pathname.startsWith(`${href}/`)

    return (
        <Link
            href={href}
            className={cn(
                "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                isActive ? "bg-red-100 text-red-700 hover:bg-red-200" : "text-slate-600 hover:bg-slate-100"
            )}
        >
            {icon}
            {children}
        </Link>
    )
}
