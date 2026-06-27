"use client"

import { LogOut } from "lucide-react"
import { useAuth } from "@/components/providers/auth-provider"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { AccountSidebarNav } from "./account-sidebar-nav"

export function GlobalLayout({ children }: { children: React.ReactNode }) {
    const { user, logout } = useAuth()

    return (
        <div className="flex h-screen border-4relative">

            {/* Sidebar */}
            <aside className="w-64 bg-dark border-r flex flex-col h-full">
                <h2 className="font-bold text-lg px-6 py-4 mb-2">Beet Account</h2>
                <ScrollArea className="flex-1">
                    <AccountSidebarNav />
                </ScrollArea>

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

