"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import { User, CreditCard, Store, LogOut } from "lucide-react"
import { useAuth } from "@/components/providers/auth-provider"
import { Button } from "@/components/ui/button"

export function GlobalLayout({ children }: { children: React.ReactNode }) {
    const { user, logout } = useAuth()

    return (
        <div className="flex h-screen border-4 border-blue-500 relative">

            {/* Sidebar */}
            <aside className="w-64 bg-dark border-r flex flex-col h-full">
                <h2 className="font-bold text-lg px-6 py-4 mb-2">Beet Account</h2>
                <nav className="space-y-1 px-4 flex-1">
                    <GlobalNavLink href="/account/restaurants" icon={<Store className="w-4 h-4" />}>
                        My Restaurants
                    </GlobalNavLink>
                    <GlobalNavLink href="/account/profile" icon={<User className="w-4 h-4" />}>
                        Profile
                    </GlobalNavLink>
                    <GlobalNavLink href="/account/billing" icon={<CreditCard className="w-4 h-4" />}>
                        Billing
                    </GlobalNavLink>
                </nav>

                {/* Logout */}
                <div className="px-4 py-3 border-t">
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
            <main className="flex-1 overflow-y-auto p-8 pt-12">
                {children}
            </main>
        </div>
    )
}

function GlobalNavLink({ href, children, icon }: { href: string, children: React.ReactNode, icon: React.ReactNode }) {
    const pathname = usePathname()
    const isActive = pathname === href
    return (
        <Link
            href={href}
            className={cn(
                "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                isActive ? "bg-blue-100 text-blue-700" : "text-slate-600 hover:bg-slate-100"
            )}
        >
            {icon}
            {children}
        </Link>
    )
}
