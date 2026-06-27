"use client"

import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { Button } from "@/components/ui/button"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { useDocumentTypes } from "@/lib/hooks/use-document-types"
import { SupplierResponse } from "@/lib/api-types"
import { SupplierRequest } from "@/lib/hooks/use-suppliers"
import { Loader2 } from "lucide-react"

const formSchema = z.object({
    name: z.string().trim().min(2, "Supplier name must be at least 2 characters."),
    documentTypeId: z.string().trim().min(1, "Document type is required."),
    documentNumber: z.string().trim().min(3, "Document number is required."),
    contactName: z.string().trim().optional(),
    email: z.string().trim().email("Email must be valid.").optional().or(z.literal("")),
    phone: z.string().trim().optional(),
    address: z.string().trim().optional(),
})

type FormValues = z.infer<typeof formSchema>

interface SupplierFormProps {
    supplier?: SupplierResponse | null
    onSubmit: (values: SupplierRequest) => void
    isSubmitting?: boolean
}

export function SupplierForm({ supplier, onSubmit, isSubmitting }: SupplierFormProps) {
    const { data: documentTypes = [], isLoading } = useDocumentTypes("CO")
    const form = useForm<FormValues>({
        resolver: zodResolver(formSchema),
        mode: "onChange",
        defaultValues: {
            name: supplier?.name ?? "",
            documentTypeId: supplier?.documentTypeId ?? "",
            documentNumber: supplier?.documentNumber ?? "",
            contactName: supplier?.contactName ?? "",
            email: supplier?.email ?? "",
            phone: supplier?.phone ?? "",
            address: supplier?.address ?? "",
        },
    })

    useEffect(() => {
        form.reset({
            name: supplier?.name ?? "",
            documentTypeId: supplier?.documentTypeId ?? "",
            documentNumber: supplier?.documentNumber ?? "",
            contactName: supplier?.contactName ?? "",
            email: supplier?.email ?? "",
            phone: supplier?.phone ?? "",
            address: supplier?.address ?? "",
        })
    }, [form, supplier])

    const handleSubmit = (values: FormValues) => {
        onSubmit({
            name: values.name,
            documentTypeId: values.documentTypeId,
            documentNumber: values.documentNumber,
            contactName: values.contactName || null,
            email: values.email || null,
            phone: values.phone || null,
            address: values.address || null,
        })
    }

    if (isLoading) {
        return (
            <div className="flex items-center justify-center py-8 text-muted-foreground">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Loading form data...
            </div>
        )
    }

    return (
        <Form {...form}>
            <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-5">
                <FormField
                    control={form.control}
                    name="name"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Name</FormLabel>
                            <FormControl>
                                <Input placeholder="e.g. Molinos del Sur" {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div className="grid grid-cols-2 gap-3">
                    <FormField
                        control={form.control}
                        name="documentTypeId"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Document Type</FormLabel>
                                <Select onValueChange={field.onChange} value={field.value}>
                                    <FormControl>
                                        <SelectTrigger>
                                            <SelectValue placeholder="Type" />
                                        </SelectTrigger>
                                    </FormControl>
                                    <SelectContent>
                                        {documentTypes.map((documentType) => (
                                            <SelectItem key={documentType.id} value={documentType.id}>
                                                {documentType.name}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                    <FormField
                        control={form.control}
                        name="documentNumber"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Document Number</FormLabel>
                                <FormControl>
                                    <Input placeholder="900123456" {...field} />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                </div>

                <FormField
                    control={form.control}
                    name="contactName"
                    render={({ field }) => (
                        <FormItem>
                            <FormLabel>Contact Name</FormLabel>
                            <FormControl>
                                <Input placeholder="Primary contact" {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div className="grid grid-cols-2 gap-3">
                    <FormField
                        control={form.control}
                        name="email"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Email</FormLabel>
                                <FormControl>
                                    <Input placeholder="supplier@example.com" {...field} />
                                </FormControl>
                                <FormMessage />
                            </FormItem>
                        )}
                    />
                    <FormField
                        control={form.control}
                        name="phone"
                        render={({ field }) => (
                            <FormItem>
                                <FormLabel>Phone</FormLabel>
                                <FormControl>
                                    <Input placeholder="+57..." {...field} />
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
                            <FormLabel>Address</FormLabel>
                            <FormControl>
                                <Textarea placeholder="Supplier address" {...field} />
                            </FormControl>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <div className="flex justify-end">
                    <Button type="submit" disabled={isSubmitting || !form.formState.isValid}>
                        {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                        {supplier ? "Save changes" : "Create supplier"}
                    </Button>
                </div>
            </form>
        </Form>
    )
}
