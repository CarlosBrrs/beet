"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
} from "@/components/ui/collapsible"
import { ChevronRight } from "lucide-react"
import { NavItem, navigationConfig } from "@/config/navigation"
import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { useRestaurantPermissions } from "@/lib/hooks/use-restaurant-permissions"
import { useState, useEffect } from "react"

interface SidebarNavProps extends React.HTMLAttributes<HTMLDivElement> {
    items?: NavItem[]
}

export function SidebarNav({ className, items = navigationConfig }: SidebarNavProps) {
    const pathname = usePathname()
    const { restaurantId } = useRestaurantContext()
    const { can } = useRestaurantPermissions(restaurantId)

    // Filter items based on permissions
    const filterNavItems = (items: NavItem[]): NavItem[] => {
        return items
            .filter((item) => {
                // If item has a specific module requirement, check 'READ' access
                // If item has a specific module requirement
                if (item.module) {
                    // We check if the user has ANY permission on this module.
                    // This is how we determine visibility ("Can I open this door?").
                    // If the user has specific actions like ["EDIT"], they implicitly can "SEE" the module link.
                    const permissions = can(item.module) // We will overload 'can' to support just checking module existence
                    if (!permissions) return false
                }
                return true
            })
            .map((item) => {
                // Recursively filter children
                if (item.items) {
                    return { ...item, items: filterNavItems(item.items) }
                }
                return item
            })
            // Remove groups that became empty after filtering children
            .filter((item) => {
                if (item.items && item.items.length === 0 && !item.href) {
                    return false
                }
                return true
            })
    }

    const filteredItems = filterNavItems(items)

    // Safety check: if no restaurantId (e.g. loading or error), 
    // we can't build links correctly.
    if (!restaurantId) return null

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
                                restaurantId={restaurantId}
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
    restaurantId
}: {
    item: NavItem;
    pathname: string;
    restaurantId: string
}) {
    // Determine the full Href (injecting restaurantId if needed)
    const resolveHref = (href?: string) => {
        if (!href) return "#"
        // Avoid double prefixing if href is absolute or already parameterized
        return `/restaurants/${restaurantId}${href}`
    }

    const href = resolveHref(item.href)
    // Check if active (simplistic matching)
    const isActive = item.href ? pathname.includes(item.href) : false

    // State for Collapsible
    const [isOpen, setIsOpen] = useState(false)

    // Auto-expand if a child is active
    useEffect(() => {
        if (item.items) {
            const hasActiveChild = item.items.some(child =>
                child.href && pathname.includes(child.href)
            )
            if (hasActiveChild) setIsOpen(true)
        }
    }, [pathname, item.items])

    // 1. Case: Section Header (Group without internal link/icon, usually just text)
    // Or simpler: We treat top-level items with children as groups
    if (item.items) {
        return (
            <div className="mb-4">
                {/* 
                   If it's a top-level group usually used as a Label (like "Operation" in the config),
                   we render a Label. 
                   NOTE: Our config structure has "Section -> Items". 
                   Let's adjust logic: If item has NO icon and HAS items, it's a Section Label.
                 */}
                {!item.icon ? (
                    <h2 className="mb-2 px-4 text-xs font-semibold tracking-tight text-muted-foreground uppercase">
                        {item.title}
                    </h2>
                ) : (
                    // It's a Collapsible Menu Item (like "Inventory" -> "Ingredients")
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
                            {/* Recursive rendering */}
                            {item.items.map((subItem, idx) => (
                                <SidebarNavItem
                                    key={idx}
                                    item={subItem}
                                    pathname={pathname}
                                    restaurantId={restaurantId}
                                />
                            ))}
                        </CollapsibleContent>
                    </Collapsible>
                )}

                {/* Render children for Section Labels (Non-collapsible style) */}
                {!item.icon && (
                    <div className="space-y-1">
                        {item.items.map((subItem, idx) => (
                            <SidebarNavItem
                                key={idx}
                                item={subItem}
                                pathname={pathname}
                                restaurantId={restaurantId}
                            />
                        ))}
                    </div>
                )}
            </div>
        )
    }

    // 2. Case: Leaf Link
    return (
        <Button
            asChild
            variant={isActive ? "secondary" : "ghost"}
            className="w-full justify-start"
        >
            <Link href={href}>
                {item.icon && <item.icon className="mr-2 h-4 w-4" />}
                {item.title}
            </Link>
        </Button>
    )
}
