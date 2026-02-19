import {
    LayoutDashboard,
    Store,
    Utensils,
    Archive,
    ShoppingCart,
    Users,
    Settings,
    FileText,
    Truck,
    ChefHat,
    Receipt,
    MonitorSmartphone,
    LayoutGrid
} from "lucide-react"
import { PermissionModule } from "@/lib/permissions"

export interface NavItem {
    title: string
    href?: string
    icon?: React.ComponentType<{ className?: string }>
    module?: PermissionModule // Permission required to view
    items?: NavItem[]
    matchPath?: string // To manually control active state matching
}

export const navigationConfig: NavItem[] = [
    {
        title: "Operation",
        items: [
            {
                title: "Dashboard",
                href: "/dashboard",
                icon: LayoutDashboard,
            },
            {
                title: "POS",
                href: "/pos",
                icon: Store,
                module: PermissionModule.ORDERS, // Changed from POS (Uses existing backend module)
            },
            {
                title: "Tables",
                href: "/tables",
                icon: LayoutGrid,
                module: PermissionModule.TABLES,
            },
            {
                title: "Live Orders (KDS)",
                href: "/orders/active",
                icon: Utensils,
                module: PermissionModule.KDS, // Updated from ORDERS
            }
        ]
    },
    {
        title: "Inventory & Catalog",
        module: PermissionModule.INVENTORY,
        items: [
            {
                title: "Ingredients",
                href: "/ingredients",
                icon: Archive,
            },
            {
                title: "Recipes",
                href: "/recipes",
                icon: ChefHat,
                module: PermissionModule.RECIPES,
            },
            {
                title: "Products",
                href: "/products",
                icon: ShoppingCart,
                module: PermissionModule.CATALOG,
            },
            {
                title: "Menus",
                href: "/menus",
                icon: FileText,
                module: PermissionModule.CATALOG,
            }
        ]
    },
    {
        title: "Management",
        items: [
            {
                title: "Sales & Reports",
                href: "/reports",
                icon: Receipt,
                module: PermissionModule.FINANCE, // Updated from RESTAURANT
            },
            {
                title: "Staff",
                href: "/staff",
                icon: Users,
                module: PermissionModule.RESTAURANTS, // Changed from STAFF to RESTAURANTS (Managing business)
            },
            {
                title: "Suppliers",
                href: "/suppliers",
                icon: Truck,
                module: PermissionModule.INVENTORY,
            }
        ]
    },
    {
        title: "Configuration",
        module: PermissionModule.RESTAURANTS, // Updated from RESTAURANT
        items: [
            {
                title: "Settings",
                href: "/settings",
                icon: Settings,
            },
            {
                title: "Devices",
                href: "/devices",
                icon: MonitorSmartphone,
                module: PermissionModule.OPERATIONS,
            },
        ]
    }
]
