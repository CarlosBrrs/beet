"use client"

import { createContext, useContext, ReactNode } from "react"

interface RestaurantState {
    restaurantId: string | null
}

const RestaurantContext = createContext<RestaurantState | undefined>(undefined)

interface RestaurantProviderProps {
    children: ReactNode
    restaurantId: string
}

/**
 * Provides the currently selected restaurantId to all children.
 * Permissions are no longer fetched here — use useMyPermissions() instead.
 */
export function RestaurantProvider({ children, restaurantId }: RestaurantProviderProps) {
    return (
        <RestaurantContext.Provider value={{ restaurantId }}>
            {children}
            {/* Context Debugger */}
            <div className="fixed bottom-2 right-2 p-1 text-[10px] bg-red-100 dark:bg-red-900 border border-red-500 rounded z-[9999] opacity-50 hover:opacity-100 transition-opacity">
                Restaurant: {restaurantId.slice(0, 8)}...
            </div>
        </RestaurantContext.Provider>
    )
}

/**
 * Returns the restaurant context. Throws if used outside a <RestaurantProvider>.
 */
export function useRestaurantContext() {
    const context = useContext(RestaurantContext)
    if (context === undefined) {
        throw new Error("useRestaurantContext must be used within a RestaurantProvider")
    }
    return context
}

/**
 * Returns the restaurant context without throwing if used outside a <RestaurantProvider>.
 * Returns undefined when not inside a restaurant layout.
 * Used by the Can component to gracefully work in both account and restaurant pages.
 */
export function useOptionalRestaurantContext() {
    return useContext(RestaurantContext)
}
