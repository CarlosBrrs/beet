"use client"

import { useEffect } from "react"
import { env } from "@/lib/env"

export function EnvDebug() {
    useEffect(() => {
        console.group("🌍 Environment Debug")
        console.log("NODE_ENV:", process.env.NODE_ENV)
        console.log("API URL:", env.NEXT_PUBLIC_API_URL)
        console.groupEnd()
    }, [])

    return null
}
