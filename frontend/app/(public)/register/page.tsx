import { RegisterForm } from "@/components/modules/auth/register-form"
import type { Metadata } from "next"

export const metadata: Metadata = {
    title: "Register | Beet SaaS",
    description: "Create your restaurant owner account",
}

export default function RegisterPage() {
    return (
        <div className="flex w-full flex-col items-center justify-center">
            <RegisterForm />
        </div>
    )
}
