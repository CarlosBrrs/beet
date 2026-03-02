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
    LayoutGrid,
    User,
    CreditCard,
    Package
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
                title: "Inventory",
                href: "/inventory",
                icon: Package,
                module: PermissionModule.INVENTORY,
            },
            {
                title: "Purchases",
                href: "/purchases",
                icon: FileText,
                module: PermissionModule.INVOICES,
            },
        ]
    },
    {
        title: "Example 1",
        items: [
            {
                title: "POS",
                href: "/pos",
                icon: Store,
                module: PermissionModule.ORDERS,
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
                module: PermissionModule.KDS,
            },
            {
                title: "Menus",
                href: "/menus",
                icon: FileText,
                module: PermissionModule.MENUS,
            }
        ]
    },
    {
        title: "Example 2",
        items: [
            {
                title: "Sales & Reports",
                href: "/reports",
                icon: Receipt,
                module: PermissionModule.FINANCE,
            },
            {
                title: "Staff",
                href: "/staff",
                icon: Users,
                module: PermissionModule.RESTAURANTS,
            }
        ]
    },
    {
        title: "Account",
        items: [
            {
                title: "Back to Account Central",
                href: "/account/restaurants",
                icon: User,
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

export const accountNavigationConfig: NavItem[] = [
    {
        title: "My Business",
        items: [
            {
                title: "Dashboard",
                href: "/account/dashboard",
                icon: LayoutDashboard,
            },
            {
                title: "Restaurants",
                href: "/account/restaurants",
                icon: Store,
            }
        ]
    },
    {
        title: "Master Catalog",
        module: PermissionModule.INVENTORY,
        items: [
            {
                title: "Ingredients",
                href: "/account/ingredients",
                icon: Archive,
            },
            {
                title: "Recipes",
                href: "/account/recipes",
                icon: ChefHat,
                module: PermissionModule.RECIPES,
            },
            {
                title: "Products",
                href: "/account/products",
                icon: ShoppingCart,
                module: PermissionModule.CATALOG,
            },
            {
                title: "Menus",
                href: "/account/menus",
                icon: FileText,
                module: PermissionModule.CATALOG,
            },
            {
                title: "Suppliers",
                href: "/account/suppliers",
                icon: Truck,
                module: PermissionModule.INVENTORY,
            }
        ]
    },
    {
        title: "Settings",
        items: [
            {
                title: "Profile",
                href: "/account/profile",
                icon: User,
            },
            {
                title: "Billing",
                href: "/account/billing",
                icon: CreditCard,
            }
        ]
    }
]
