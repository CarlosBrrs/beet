import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ChefHat, ArrowRight } from "lucide-react";

export default function Home() {
    return (
        <div className="flex min-h-screen flex-col bg-background">
            {/* Navbar / Header */}
            <header className="sticky top-0 z-40 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
                <div className="container mx-auto flex h-16 items-center justify-between px-4 md:px-6">
                    <div className="flex items-center gap-2 font-bold text-xl tracking-tighter">
                        <ChefHat className="h-6 w-6 text-primary" />
                        <span>Beet</span>
                    </div>
                    <nav className="flex items-center gap-4">
                        <Link href="/login">
                            <Button variant="ghost" size="sm">Login</Button>
                        </Link>
                        <Link href="/register">
                            <Button size="sm">Get Started</Button>
                        </Link>
                    </nav>
                </div>
            </header>

            {/* Hero Section */}
            <main className="flex-1 flex items-center justify-center">
                <section className="w-full py-12 md:py-24 lg:py-32 xl:py-48">
                    <div className="container mx-auto px-4 md:px-6">
                        <div className="grid gap-6 lg:grid-cols-[1fr_400px] lg:gap-12 xl:grid-cols-[1fr_600px] items-center">
                            <div className="flex flex-col justify-center space-y-4">
                                <div className="space-y-2">
                                    <h1 className="text-3xl font-bold tracking-tighter sm:text-5xl xl:text-6xl/none">
                                        The Modern OS for fast-paced Restaurants
                                    </h1>
                                    <p className="max-w-[600px] text-muted-foreground md:text-xl">
                                        Manage inventory, staff, and orders in one seamless platform. Built for scale, designed for speed.
                                    </p>
                                </div>
                                <div className="flex flex-col gap-2 min-[400px]:flex-row">
                                    <Link href="/register">
                                        <Button size="lg" className="gap-2">
                                            Start for Free <ArrowRight className="h-4 w-4" />
                                        </Button>
                                    </Link>
                                    <Link href="/login">
                                        <Button variant="outline" size="lg">
                                            Existing User
                                        </Button>
                                    </Link>
                                </div>
                            </div>

                            {/* Hero Image / Graphic Placeholder */}
                            <div className="flex items-center justify-center">
                                <Card className="w-full max-w-md shadow-2xl bg-card/50 backdrop-blur-sm border-muted/40">
                                    <CardHeader>
                                        <CardTitle>Kitchen Display System</CardTitle>
                                        <CardDescription>Live real-time updates</CardDescription>
                                    </CardHeader>
                                    <CardContent className="space-y-4">
                                        <div className="flex items-center justify-between rounded-lg border p-4 bg-background">
                                            <div className="space-y-0.5">
                                                <div className="text-sm font-medium">Table 12</div>
                                                <div className="text-xs text-muted-foreground">Ordered 2m ago</div>
                                            </div>
                                            <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 border-transparent bg-primary text-primary-foreground shadow hover:bg-primary/80">Pending</span>
                                        </div>
                                        <div className="flex items-center justify-between rounded-lg border p-4 bg-background opacity-60">
                                            <div className="space-y-0.5">
                                                <div className="text-sm font-medium">Table 04</div>
                                                <div className="text-xs text-muted-foreground">Ordered 15m ago</div>
                                            </div>
                                            <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold text-foreground">Completed</span>
                                        </div>
                                    </CardContent>
                                </Card>
                            </div>
                        </div>
                    </div>
                </section>
            </main>

            <footer className="py-6 w-full shrink-0 items-center px-4 md:px-6 border-t font-mono text-sm text-muted-foreground text-center">
                <p>&copy; 2026 Beet SaaS. All rights reserved.</p>
            </footer>
        </div>
    );
}