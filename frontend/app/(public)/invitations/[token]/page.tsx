"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import { useParams, useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { toast } from "sonner"
import * as z from "zod"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useAcceptInvitation, useInvitationPreview } from "@/lib/hooks/use-staff"

const schema = z.object({
    firstName: z.string().min(1, "Required"),
    secondName: z.string().optional(),
    firstLastname: z.string().min(1, "Required"),
    secondLastname: z.string().optional(),
    phoneNumber: z.string().optional(),
    username: z.string().optional(),
    password: z.string().min(8, "At least 8 characters"),
})

type FormValues = z.infer<typeof schema>

export default function AcceptInvitationPage() {
    const params = useParams<{ token: string }>()
    const router = useRouter()
    const token = params.token
    const preview = useInvitationPreview(token)
    const accept = useAcceptInvitation(token)
    const form = useForm<FormValues>({
        resolver: zodResolver(schema),
        defaultValues: {
            firstName: "",
            secondName: "",
            firstLastname: "",
            secondLastname: "",
            phoneNumber: "",
            username: "",
            password: "",
        },
    })

    const onSubmit = async (values: FormValues) => {
        try {
            await accept.mutateAsync(values)
            toast.success("Invitation accepted. You can now log in.")
            router.push("/login")
        } catch (error) {
            toast.error(error instanceof Error ? error.message : "Could not accept invitation")
        }
    }

    return (
        <div className="flex min-h-screen items-center justify-center bg-muted/30 p-4">
            <Card className="w-full max-w-xl rounded-none">
                <CardHeader>
                    <CardTitle>Accept staff invitation</CardTitle>
                    {preview.data && (
                        <p className="text-sm text-muted-foreground">
                            {preview.data.restaurantName} · {preview.data.roleName} · {preview.data.email}
                        </p>
                    )}
                </CardHeader>
                <CardContent>
                    {preview.isLoading && <p className="text-sm text-muted-foreground">Loading invitation...</p>}
                    {preview.isError && <p className="text-sm text-destructive">This invitation is not available.</p>}
                    {preview.data?.status !== "PENDING" && preview.data && (
                        <p className="text-sm text-destructive">This invitation is {preview.data.status.toLowerCase()}.</p>
                    )}
                    {preview.data?.status === "PENDING" && (
                        <Form {...form}>
                            <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
                                <div className="grid gap-4 sm:grid-cols-2">
                                    <TextField control={form.control} name="firstName" label="First name" />
                                    <TextField control={form.control} name="secondName" label="Second name" />
                                    <TextField control={form.control} name="firstLastname" label="First lastname" />
                                    <TextField control={form.control} name="secondLastname" label="Second lastname" />
                                    <TextField control={form.control} name="phoneNumber" label="Phone" />
                                    <TextField control={form.control} name="username" label="Username" />
                                </div>
                                <TextField control={form.control} name="password" label="Password" type="password" />
                                <Button type="submit" className="w-full" disabled={accept.isPending}>
                                    Accept invitation
                                </Button>
                            </form>
                        </Form>
                    )}
                </CardContent>
            </Card>
        </div>
    )
}

function TextField({
    control, name, label, type = "text",
}: {
    control: ReturnType<typeof useForm<FormValues>>["control"]
    name: keyof FormValues
    label: string
    type?: string
}) {
    return (
        <FormField
            control={control}
            name={name}
            render={({ field }) => (
                <FormItem>
                    <FormLabel>{label}</FormLabel>
                    <FormControl><Input type={type} {...field} /></FormControl>
                    <FormMessage />
                </FormItem>
            )}
        />
    )
}
