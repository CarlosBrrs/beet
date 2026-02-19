import { RestaurantList } from "@/components/modules/restaurants/restaurant-list"
import { Metadata } from "next"

export const metadata: Metadata = {
    title: "My Restaurants | Beet",
}

export default function MyRestaurantsPage() {
    return <RestaurantList />
}
