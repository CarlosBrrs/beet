import { GlobalLayout } from "@/components/layouts/global-layout"

export default function AccountLayout({ children }: { children: React.ReactNode }) {
    return (
        <GlobalLayout>
            {children}
        </GlobalLayout>
    )
}
