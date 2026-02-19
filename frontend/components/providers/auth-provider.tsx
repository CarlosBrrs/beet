"use client"

import { createContext, useContext, useEffect, useState } from "react"
import { useRouter, usePathname } from "next/navigation"
import { jwtDecode } from "jwt-decode"
import { UserResponse } from "@/lib/api-types"
import { toast } from "sonner"

interface AuthContextType {
    user: UserResponse | null
    token: string | null
    login: (token: string, user: UserResponse) => void
    logout: () => void
    isAuthenticated: boolean
    isLoading: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

const TOKEN_KEY = "beet_token"
const USER_KEY = "beet_user"

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [user, setUser] = useState<UserResponse | null>(null)
    const [token, setToken] = useState<string | null>(null)
    const [isLoading, setIsLoading] = useState(true)
    const router = useRouter()

    useEffect(() => {
        // Hydrate auth state from localStorage
        const storedToken = localStorage.getItem(TOKEN_KEY)
        const storedUser = localStorage.getItem(USER_KEY)

        if (storedToken && storedUser) {
            try {
                const decoded: any = jwtDecode(storedToken)
                const currentTime = Date.now() / 1000

                if (decoded.exp < currentTime) {
                    // Token expired
                    logout()
                    toast.error("Session expired. Please login again.")
                } else {
                    setToken(storedToken)
                    setUser(JSON.parse(storedUser))
                }
            } catch (error) {
                console.error("Invalid token:", error)
                logout()
            }
        }
        setIsLoading(false)
    }, [])

    const login = (newToken: string, newUser: UserResponse) => {
        localStorage.setItem(TOKEN_KEY, newToken)
        localStorage.setItem(USER_KEY, JSON.stringify(newUser))
        setToken(newToken)
        setUser(newUser)
        toast.success(`Welcome back, ${newUser.firstName}!`)
        router.push("/dashboard")
    }

    const logout = () => {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
        setToken(null)
        setUser(null)
        router.push("/login")
    }

    // Optional: Intercept 401s globally (simpler to do in API wrappers, but good to know)

    return (
        <AuthContext.Provider
            value={{
                user,
                token,
                login,
                logout,
                isAuthenticated: !!user,
                isLoading,
            }}
        >
            {children}
        </AuthContext.Provider>
    )
}

export const useAuth = () => {
    const context = useContext(AuthContext)
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider")
    }
    return context
}
