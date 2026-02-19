import { redirect } from "next/navigation"

export default function DashboardRoot() {
    redirect("/account/restaurants")
}
