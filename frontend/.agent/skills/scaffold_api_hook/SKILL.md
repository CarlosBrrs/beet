---
name: scaffold_api_hook
description: Generates a custom hook using TanStack Query (useQuery/useMutation) for standardized data fetching.
---

# Scaffold API Hook

This skill generates a typed data fetching hook using `@tanstack/react-query`.

## Usage

User will provide: `ResourceName` (e.g., `RestaurantPermissions`, `Menu`)

## Actions

1.  **Create Hook File**:
    *   Path: `lib/hooks/use-[resource-name].ts`

## Template

```tsx
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { env } from "@/lib/env"
import { ApiGenericResponse } from "@/lib/api-types"

// Define Types
export interface [ResourceName] {
    id: string
    // Add other fields
}

// Query Keys
export const [ResourceName]Keys = {
    all: ["[resource-name]"] as const,
    lists: () => [...[ResourceName]Keys.all, "list"] as const,
    list: (filter: string) => [...[ResourceName]Keys.lists(), { filter }] as const,
    details: () => [...[ResourceName]Keys.all, "detail"] as const,
    detail: (id: string) => [...[ResourceName]Keys.details(), id] as const,
}

// Fetcher Function
async function fetch[ResourceName]s(): Promise<[ResourceName][]> {
    const res = await fetch(`${env.NEXT_PUBLIC_API_URL}/[resource-name]s`)
    if (!res.ok) throw new Error("Failed to fetch [resource-name]s")
    const data: ApiGenericResponse<[ResourceName][]> = await res.json()
    return data.data
}

// Hook
export function use[ResourceName]s() {
    return useQuery({
        queryKey: [ResourceName]Keys.lists(),
        queryFn: fetch[ResourceName]s,
    })
}

// Example Mutation (Create)
async function create[ResourceName](newItem: Partial<[ResourceName]>) {
    const res = await fetch(`${env.NEXT_PUBLIC_API_URL}/[resource-name]s`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(newItem),
    })
    if (!res.ok) throw new Error("Failed to create [resource-name]")
    return res.json()
}

export function useCreate[ResourceName]() {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: create[ResourceName],
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: [ResourceName]Keys.lists() })
        },
    })
}
```
