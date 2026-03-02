"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { Button } from "@/components/ui/button"
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "@/components/ui/card"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { toast } from "sonner"
import { useState, useEffect } from "react"
import { Loader2 } from "lucide-react"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { env } from "@/lib/env"
import { ApiGenericResponse, SubscriptionPlan } from "@/lib/api-types"

const registerSchema = z.object({
    firstName: z.string().trim().min(2, "First name is required"),
    secondName: z.string().trim().optional(),
    firstLastname: z.string().trim().min(2, "Last name is required"),
    secondLastname: z.string().trim().optional(),
    username: z.string().trim().min(3, "Username must be at least 3 characters"),
    email: z.string().trim().email("Invalid email address"),
    phoneNumber: z.string().trim().min(10, "Phone number must be at least 10 digits"),
    password: z.string().trim().min(8, "Password must be at least 8 characters"),
    subscriptionPlanId: z.string().trim().uuid("Please select a subscription plan"),
})

export function RegisterForm() {
    const [isLoading, setIsLoading] = useState(false)
    const [plans, setPlans] = useState<SubscriptionPlan[]>([])
    const [loadingPlans, setLoadingPlans] = useState(true)

    useEffect(() => {
        async function fetchPlans() {
            try {
                const res = await fetch(`${env.NEXT_PUBLIC_API_URL}/subscriptions/plans`)
                if (!res.ok) throw new Error("Failed to fetch plans")
                const data: ApiGenericResponse<SubscriptionPlan[]> = await res.json()
                if (data.success) {
                    setPlans(data.data)
                } else {
                    toast.error(data.errorMessage || "Failed to load subscription plans")
                }
            } catch (error) {
                toast.error("Could not load subscription plans. Is the backend running?")
                console.error(error)
            } finally {
                setLoadingPlans(false)
            }
        }
        fetchPlans()
    }, [])

    const form = useForm<z.infer<typeof registerSchema>>({
        resolver: zodResolver(registerSchema),
        defaultValues: {
            firstName: "",
            secondName: "",
            firstLastname: "",
            secondLastname: "",
            username: "",
            email: "",
            phoneNumber: "",
            password: "",
            subscriptionPlanId: "",
        },
    })

    const router = useRouter()

    async function onSubmit(values: z.infer<typeof registerSchema>) {
        setIsLoading(true)
        try {
            await new Promise(resolve => setTimeout(resolve, 3000)) // 3s delay

            const res = await fetch(`${env.NEXT_PUBLIC_API_URL}/auth/register`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(values),
            })

            const data = await res.json()

            if (!res.ok || !data.success) {
                toast.error(data.errorMessage || "Registration failed")
                return
            }

            toast.success("Account created successfully! Redirecting to login...")
            router.push("/login")
        } catch (error) {
            toast.error("An unexpected error occurred")
            console.error(error)
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <Card className="w-full max-w-lg">
            <CardHeader>
                <CardTitle>Create an Account</CardTitle>
                <CardDescription>Start managing your restaurant today.</CardDescription>
            </CardHeader>
            <CardContent>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">

                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={form.control}
                                name="firstName"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>First Name</FormLabel>
                                        <FormControl>
                                            <Input placeholder="John" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="secondName"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Middle Name (Opt)</FormLabel>
                                        <FormControl>
                                            <Input placeholder="" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={form.control}
                                name="firstLastname"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Last Name</FormLabel>
                                        <FormControl>
                                            <Input placeholder="Doe" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="secondLastname"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>2nd Last Name (Opt)</FormLabel>
                                        <FormControl>
                                            <Input placeholder="" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <FormField
                            control={form.control}
                            name="username"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Username</FormLabel>
                                    <FormControl>
                                        <Input placeholder="johndoe123" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={form.control}
                                name="email"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Email</FormLabel>
                                        <FormControl>
                                            <Input placeholder="john@example.com" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="phoneNumber"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Phone</FormLabel>
                                        <FormControl>
                                            <Input placeholder="+1234567890" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <FormField
                            control={form.control}
                            name="password"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Password</FormLabel>
                                    <FormControl>
                                        <Input type="password" placeholder="******" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="subscriptionPlanId"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Subscription Plan</FormLabel>
                                    <Select onValueChange={field.onChange} defaultValue={field.value} disabled={loadingPlans}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder={loadingPlans ? "Loading plans..." : "Select a plan"} />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            {plans.map((plan) => (
                                                <SelectItem key={plan.id} value={plan.id}>
                                                    {plan.name} - ${plan.price}/{plan.interval === 'MONTHLY' ? 'mo' : 'yr'}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <Button type="submit" className="w-full" disabled={isLoading || !form.formState.isValid}>
                            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                            {isLoading ? "Creating..." : "Create Account"}
                        </Button>
                    </form>
                </Form>
            </CardContent>
            <CardFooter className="flex justify-center">
                <p className="text-sm text-muted-foreground">
                    Already have an account?{" "}
                    <Link href="/login" className="text-primary hover:underline">
                        Login here
                    </Link>
                </p>
            </CardFooter>
        </Card>
    )
}
