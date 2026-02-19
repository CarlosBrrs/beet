---
name: scaffold_react_context
description: Generates a new React Context Provider with types and specific custom hooks.
---

# Scaffold React Context

This skill generates a robust React Context Provider.

## Usage

User will provide: `ContextName` (e.g., `Restaurant`, `OrderEntry`)

## Actions

1.  **Create Provider File**:
    *   Path: `components/providers/[context-name]-provider.tsx`

## Template

```tsx
"use client"

import { createContext, useContext, ReactNode, useState } from "react"

// Replace 'ItemType' with the actual type of items managed by this context
interface [ContextName]State<T> {
    items: T[]
    isLoading: boolean
    isError: boolean
    error: Error | null
}

interface [ContextName]Actions<T> {
    addItem: (item: T) => void
}

type [ContextName]ContextType<T> = [ContextName]State<T> & [ContextName]Actions<T>

// Create context with undefined initial value
const [ContextName]Context = createContext<[ContextName]ContextType<any> | undefined>(undefined)

interface [ContextName]ProviderProps {
    children: ReactNode
}

// Define the ItemType here or import it
type ItemType = unknown // TODO: Replace with actual type, e.g., Order, Product

export function [ContextName]Provider({ children }: [ContextName]ProviderProps) {
    const [isLoading, setIsLoading] = useState(false)
    const [isError, setIsError] = useState(false)
    const [error, setError] = useState<Error | null>(null)
    const [items, setItems] = useState<ItemType[]>([])

    const addItem = (item: ItemType) => {
        console.log("Add item", item)
        // Implement logic
    }

    const value = {
        items,
        isLoading,
        isError,
        error,
        addItem
    }

    return (
        <[ContextName]Context.Provider value={value}>
            {children}
        </[ContextName]Context.Provider>
    )
}

export function use[ContextName]() {
    const context = useContext([ContextName]Context)
    if (context === undefined) {
        throw new Error("use[ContextName] must be used within a [ContextName]Provider")
    }
    return context
}
```
