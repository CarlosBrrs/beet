"use client"

import { useRestaurantContext } from "@/components/providers/restaurant-provider"
import { PermissionAction, PermissionModule } from "@/lib/permissions";
import { useRestaurantPermissions } from "@/lib/hooks/use-restaurant-permissions";
import React from "react"; // Added for React.ReactNode

interface CanProps {
    I: PermissionAction | string;
    a: PermissionModule | string;
    children: React.ReactNode;
    fallback?: React.ReactNode;
}

export function Can({ I, a, children, fallback = null }: CanProps) {
    const { restaurantId } = useRestaurantContext();
    // We use the same hook! No code duplication.
    const { can, isLoading } = useRestaurantPermissions(restaurantId);

    if (isLoading) return null; // Or a skeleton if critical

    if (can(I, a)) {
        return <>{children}</>;
    }

}
