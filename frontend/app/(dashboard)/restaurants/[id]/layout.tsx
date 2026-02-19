import { RestaurantLayout } from "@/components/layouts/restaurant-layout"

export default async function Layout({
    children,
    params
}: {
    children: React.ReactNode
    params: Promise<{ id: string }>
}) {
    // Next.js 15: params is a Promise
    const { id } = await params

    return (
        <RestaurantLayout params={{ id }}>
            {children}
        </RestaurantLayout>
    )
}
