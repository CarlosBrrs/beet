"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Store, Loader2, ArrowLeft } from "lucide-react"
import { toast } from "sonner"
import Link from "next/link"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
    Form,
    FormControl,
    FormDescription,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { apiClient } from "@/lib/api-client"
import { ApiGenericResponse, RestaurantResponse } from "@/lib/api-types"

const createRestaurantSchema = z.object({
    name: z.string().min(3, "Name must be at least 3 characters"),
    operationMode: z.enum(["PREPAID", "POSTPAID"]),
    phoneNumber: z.string().optional(),
    address: z.string().optional(),
    email: z.email().optional().or(z.literal("")),
    settings: z.object({
        prePaymentEnabled: z.boolean(),
        allowTakeaway: z.boolean(),
        allowDelivery: z.boolean(),
        maxTableCapacity: z.coerce.number<number>().int().min(1, "Capacity must be at least 1"),
    })
})

type CreateRestaurantValues = z.infer<typeof createRestaurantSchema>

export default function CreateRestaurantPage() {
    const router = useRouter()
    const [isLoading, setIsLoading] = useState(false)

    const form = useForm<CreateRestaurantValues>({
        resolver: zodResolver(createRestaurantSchema),
        defaultValues: {
            name: "",
            operationMode: "PREPAID",
            phoneNumber: "",
            address: "",
            email: "",
            settings: {
                prePaymentEnabled: false,
                allowTakeaway: true,
                allowDelivery: true,
                maxTableCapacity: 1,
            }
        },
    })

async function onSubmit(values: CreateRestaurantValues) {
    setIsLoading(true)
    try {
        const res = await apiClient<ApiGenericResponse<RestaurantResponse>>("/restaurants", {
            method: "POST",
            body: JSON.stringify(values),
        })

        toast.success("Restaurant created successfully!")
        router.push(`/restaurants/${res.data.id}/dashboard`)
    } catch (error: any) {
        toast.error(error.message || "Failed to create restaurant")
    } finally {
        setIsLoading(false)
    }
}

return (
    <div className="container max-w-2xl py-10">
        <div className="mb-6">
            <Button variant="ghost" asChild className="pl-0 hover:bg-transparent hover:text-primary">
                <Link href="/account/restaurants">
                    <ArrowLeft className="mr-2 h-4 w-4" />
                    Back to My Restaurants
                </Link>
            </Button>
        </div>

        <Card>
            <CardHeader>
                <CardTitle className="text-2xl flex items-center gap-2">
                    <Store className="h-6 w-6" />
                    Create New Restaurant
                </CardTitle>
                <CardDescription>
                    Set up your new workspace. You will be assigned as the owner.
                </CardDescription>
            </CardHeader>
            <CardContent>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                        <FormField
                            control={form.control}
                            name="name"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Restaurant Name</FormLabel>
                                    <FormControl>
                                        <Input placeholder="e.g. Luigi's Trattoria" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={form.control}
                            name="operationMode"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Operation Mode</FormLabel>
                                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder="Select mode" />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            <SelectItem value="PREPAID">Pre-Paid (Fast Food)</SelectItem>
                                            <SelectItem value="POSTPAID">Post-Paid (Dine In)</SelectItem>
                                        </SelectContent>
                                    </Select>
                                    <FormDescription>
                                        How do you operate? This affects available features.
                                    </FormDescription>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <FormField
                                control={form.control}
                                name="phoneNumber"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Phone (Optional)</FormLabel>
                                        <FormControl>
                                            <Input placeholder="+1 234..." {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="email"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>Business Email (Optional)</FormLabel>
                                        <FormControl>
                                            <Input placeholder="contact@restaurant.com" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <FormField
                            control={form.control}
                            name="address"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>Address (Optional)</FormLabel>
                                    <FormControl>
                                        <Input placeholder="123 Main St..." {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <div className="space-y-4 border p-4 rounded-md">
                            <h3 className="font-medium">Restaurant Settings</h3>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <FormField
                                    control={form.control}
                                    name="settings.maxTableCapacity"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>Max Table Capacity</FormLabel>
                                            <FormControl>
                                                <Input type="number" {...field} />
                                            </FormControl>
                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />
                                {/* Switches for booleans can be added later if needed, for now using simple checkboxes or defaulting */}
                            </div>
                        </div>

                        <Button type="submit" className="w-full" disabled={isLoading}>
                            {isLoading ? (
                                <>
                                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                                    Creating...
                                </>
                            ) : (
                                "Create Restaurant"
                            )}
                        </Button>
                    </form>
                </Form>
            </CardContent>
        </Card>
    </div>
)
}
