import { env } from "@/lib/env"
import { getOrCreateDeviceId } from "@/lib/device-id"

const TOKEN_KEY = "beet_token"

interface FetchOptions extends RequestInit {
    headers?: Record<string, string>
}

export class ApiClientError extends Error {
    constructor(message: string, public readonly status: number) {
        super(message)
        this.name = "ApiClientError"
    }
}

export async function apiClient<T>(endpoint: string, options: FetchOptions = {}): Promise<T> {
    const token = localStorage.getItem(TOKEN_KEY)

    const headers: Record<string, string> = {
        "Content-Type": "application/json",
        "X-Device-Id": getOrCreateDeviceId(),
        ...options.headers,
    }

    if (token) {
        headers["Authorization"] = `Bearer ${token}`
    }

    const config = {
        ...options,
        headers,
    }

    const response = await fetch(`${env.NEXT_PUBLIC_API_URL}${endpoint}`, config)

    if (response.status === 401) {
        // Handle unauthorized (redirect to login or clear token)
        // Since this is a utility, we might dispatch an event or just let the caller handle it.
        // For now, let's allow 401 to propagate but user can handle redirect.
        // Optionally: window.location.href = '/login' (Aggressive but effective)
    }

    // Try to parse JSON, but handle empty responses
    let data: unknown = null
    const contentType = response.headers.get("content-type")
    if (contentType && contentType.includes("application/json")) {
        data = await response.json()
    }

    if (response.ok) {
        return data as T
    }

    const errorMessage = data && typeof data === "object" && "errorMessage" in data
        ? String(data.errorMessage)
        : null

    throw new ApiClientError(errorMessage || response.statusText || "API Request Failed", response.status)
}
