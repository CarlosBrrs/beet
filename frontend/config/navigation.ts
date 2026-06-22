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
    LayoutGrid,
    User,
    CreditCard,
    Package,
    CircleDollarSign,
    Blocks,
    BarChart3
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
            {
                title: "Cash Registers",
                href: "/cash-registers",
                icon: CircleDollarSign,
                module: PermissionModule.CASH,
            },
            {
                title: "Tables",
                href: "/tables",
                icon: LayoutGrid,
                module: PermissionModule.TABLES,
            },
            {
                title: "POS",
                href: "/pos",
                icon: Store,
                module: PermissionModule.ORDERS,
            },
            {
                title: "Orders",
                href: "/orders",
                icon: Receipt,
                module: PermissionModule.ORDERS,
            },
            {
                title: "Live Orders (KDS)",
                href: "/orders/active",
                icon: Utensils,
                module: PermissionModule.KDS,
            },
            {
                title: "Payment Methods",
                href: "/payment-methods",
                icon: CreditCard,
                module: PermissionModule.PAYMENTS,
            },
        ]
    },
    {
        title: "Catalog",
        items: [
            {
                title: "Menus",
                href: "/menus",
                icon: FileText,
                module: PermissionModule.MENUS,
            },
            {
                title: "Preparaciones",
                href: "/preparations",
                icon: ChefHat,
                module: PermissionModule.PREPARATIONS,
            },
            {
                title: "Productos",
                href: "/products",
                icon: Package,
                module: PermissionModule.PRODUCTS,
            },
            {
                title: "Armables",
                href: "/templates",
                icon: Blocks,
                module: PermissionModule.TEMPLATES,
            },
        ]
    },
    {
        title: "Reports",
        items: [
            {
                title: "Operational Reports",
                href: "/reports",
                icon: BarChart3,
                module: PermissionModule.FINANCE,
            },
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
                title: "Staff",
                href: "/staff",
                icon: Users,
                module: PermissionModule.STAFF,
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
            },
            {
                title: "Cash Sessions",
                href: "/account/cash-sessions",
                icon: CircleDollarSign,
                module: PermissionModule.CASH,
            },
            {
                title: "Reports",
                href: "/account/reports",
                icon: BarChart3,
            },
            {
                title: "Staff",
                href: "/account/staff",
                icon: Users,
                module: PermissionModule.STAFF,
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
                module: PermissionModule.PREPARATIONS,
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

