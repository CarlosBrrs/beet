"use client"

import { createContext, useContext, ReactNode } from "react"
import { useRestaurantPermissions } from "@/lib/hooks/use-restaurant-permissions"
import { PermissionMap } from "@/lib/permissions"

interface RestaurantState {
    restaurantId: string | null
    roleName: string | null
    permissions: PermissionMap | null
    isLoading: boolean
    isError: boolean
    error: Error | null
}

const RestaurantContext = createContext<RestaurantState | undefined>(undefined)

interface RestaurantProviderProps {
    children: ReactNode
    restaurantId: string
}

export function RestaurantProvider({ children, restaurantId }: RestaurantProviderProps) {
    const { data, isLoading, isError, error } = useRestaurantPermissions(restaurantId)

    const value: RestaurantState = {
        restaurantId,
        roleName: data?.roleName ?? null,
        permissions: data?.permissions ?? null,
        isLoading,
        isError,
        error: error as Error | null,
    }

    return (
        <RestaurantContext.Provider value={value}>
            {children}
            {/* Context Debugger (Hidden in production or subtle) */}
            <div className="fixed bottom-2 right-2 p-1 text-[10px] bg-red-100 dark:bg-red-900 border border-red-500 rounded z-[9999] opacity-50 hover:opacity-100 transition-opacity">
                Restaurant: {restaurantId.slice(0, 8)}... | Role: {value.roleName || "Loading..."}
            </div>
        </RestaurantContext.Provider>
    )
}

export function useRestaurantContext() {
    const context = useContext(RestaurantContext)
    if (context === undefined) {
        throw new Error("useRestaurantContext must be used within a RestaurantProvider")
    }
    return context
}
