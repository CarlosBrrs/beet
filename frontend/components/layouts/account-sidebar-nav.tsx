"use client"

import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
} from "@/components/ui/collapsible"
import { ChevronRight } from "lucide-react"
import { NavItem, accountNavigationConfig } from "@/config/navigation"
import { useMyPermissions } from "@/lib/hooks/use-my-permissions"
import { useState, useEffect } from "react"
import Link from "next/link"

interface AccountSidebarNavProps extends React.HTMLAttributes<HTMLDivElement> {
    items?: NavItem[]
}

export function AccountSidebarNav({ className, items = accountNavigationConfig }: AccountSidebarNavProps) {
    const pathname = usePathname()
    const { can } = useMyPermissions()

    // Filter items based on owner permissions.
    // Account-level items with a module requirement check against the global (null) scope.
    // TODO: Analyze if nav config or Can component should be the single source of truth for this.
    const filterNavItems = (items: NavItem[]): NavItem[] => {
        return items
            .filter((item) => {
                if (item.module) {
                    // null restaurantId = account-level / owner scope check
                    return can("VIEW", item.module, null)
                }
                return true
            })
            .map((item) => {
                if (item.items) {
                    return { ...item, items: filterNavItems(item.items) }
                }
                return item
            })
            .filter((item) => {
                if (item.items && item.items.length === 0 && !item.href) return false
                return true
            })
    }

    const filteredItems = filterNavItems(items)

    return (
        <div className={cn("pb-12", className)}>
            <div className="space-y-4 py-4">
                <div className="px-3 py-2">
                    <div className="space-y-1">
                        {filteredItems.map((item, index) => (
                            <SidebarNavItem
                                key={index}
                                item={item}
                                pathname={pathname}
                            />
                        ))}
                    </div>
                </div>
            </div>
        </div>
    )
}

function SidebarNavItem({
    item,
    pathname,
}: {
    item: NavItem;
    pathname: string;
}) {
    // Account links should inherently be absolute (e.g. /account/restaurants, /account/ingredients)
    const href = item.href || "#"
    const isActive = item.href ? pathname.includes(item.href) : false

    const [isOpen, setIsOpen] = useState(false)

    useEffect(() => {
        if (item.items) {
            const hasActiveChild = item.items.some(child =>
                child.href && pathname.includes(child.href)
            )
            if (hasActiveChild) setIsOpen(true)
        }
    }, [pathname, item.items])

    if (item.items) {
        return (
            <div className="mb-4">
                {!item.icon ? (
                    <h2 className="mb-2 px-4 text-xs font-semibold tracking-tight text-muted-foreground uppercase">
                        {item.title}
                    </h2>
                ) : (
                    <Collapsible open={isOpen} onOpenChange={setIsOpen} className="w-full">
                        <CollapsibleTrigger asChild>
                            <Button variant="ghost" className="w-full justify-between font-normal hover:bg-muted/50">
                                <span className="flex items-center">
                                    {item.icon && <item.icon className="mr-2 h-4 w-4" />}
                                    {item.title}
                                </span>
                                <ChevronRight className={cn("h-4 w-4 transition-transform", isOpen && "rotate-90")} />
                            </Button>
                        </CollapsibleTrigger>
                        <CollapsibleContent className="pl-4 space-y-1 mt-1">
                            {item.items.map((subItem, idx) => (
                                <SidebarNavItem
                                    key={idx}
                                    item={subItem}
                                    pathname={pathname}
                                />
                            ))}
                        </CollapsibleContent>
                    </Collapsible>
                )}

                {!item.icon && (
                    <div className="space-y-1">
                        {item.items.map((subItem, idx) => (
                            <SidebarNavItem
                                key={idx}
                                item={subItem}
                                pathname={pathname}
                            />
                        ))}
                    </div>
                )}
            </div>
        )
    }

    // Normal link
    return (
        <Button
            asChild
            variant={isActive ? "secondary" : "ghost"}
            className={cn(
                "w-full justify-start",
                isActive ? "bg-blue-100 text-blue-700 hover:bg-blue-100 hover:text-blue-700" : "hover:bg-muted/50"
            )}
        >
            <Link href={href}>
                {item.icon && <item.icon className="mr-2 h-4 w-4" />}
                {item.title}
            </Link>
        </Button>
    )
}
