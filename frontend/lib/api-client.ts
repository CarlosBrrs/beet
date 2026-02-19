import { env } from "@/lib/env"

const TOKEN_KEY = "beet_token"

interface FetchOptions extends RequestInit {
    headers?: Record<string, string>
}

export async function apiClient<T>(endpoint: string, options: FetchOptions = {}): Promise<T> {
    const token = localStorage.getItem(TOKEN_KEY)

    const headers: Record<string, string> = {
        "Content-Type": "application/json",
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
    let data: any = null
    const contentType = response.headers.get("content-type")
    if (contentType && contentType.includes("application/json")) {
        data = await response.json()
    }

    if (response.ok) {
        return data as T
    }

    throw new Error(data?.errorMessage || response.statusText || "API Request Failed")
}
