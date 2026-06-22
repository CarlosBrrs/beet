"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { Bell, BellOff, ChefHat, Check, Clock, Flame, UserRound, Wifi, WifiOff, X } from "lucide-react"

import { ListPagination } from "@/components/shared/list-pagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { KitchenTicketResponse, KitchenTicketStatus, KitchenTicketStreamEvent } from "@/lib/api-types"
import { useKitchenTickets, useKitchenTicketStream, useUpdateKitchenTicketStatus } from "@/lib/hooks/use-orders"
import { useNow } from "@/lib/hooks/use-now"
import { cn } from "@/lib/utils"

const COLUMN_SIZE = 20
const HIGHLIGHT_MS = 45_000
const SOUND_PROMPT_KEY = "beet_kds_sound_prompt_dismissed"

type BoardStatus = Extract<KitchenTicketStatus, "PENDING" | "PREPARING" | "READY">
type AudioWindow = Window & { webkitAudioContext?: typeof AudioContext }

const columns: Array<{ status: BoardStatus; title: string; description: string }> = [
    { status: "PENDING", title: "Pendientes", description: "Tickets nuevos por iniciar" },
    { status: "PREPARING", title: "Preparando", description: "Trabajo activo en cocina" },
    { status: "READY", title: "Listos", description: "Terminados para entregar" },
]

export default function OrdersActivePage() {
    const [pages, setPages] = useState<Record<BoardStatus, number>>({ PENDING: 0, PREPARING: 0, READY: 0 })
    const [soundEnabled, setSoundEnabled] = useState(false)
    const [soundPromptDismissed, setSoundPromptDismissed] = useState(() =>
        typeof window !== "undefined" && sessionStorage.getItem(SOUND_PROMPT_KEY) === "true"
    )
    const [audioError, setAudioError] = useState<string | null>(null)
    const [highlightedTickets, setHighlightedTickets] = useState<Set<string>>(() => new Set())
    const audioContextRef = useRef<AudioContext | null>(null)
    const soundEnabledRef = useRef(false)
    const highlightTimersRef = useRef<Map<string, number>>(new Map())
    const now = useNow(15_000)
    const updateStatus = useUpdateKitchenTicketStatus()

    const pending = useKitchenTickets({ page: pages.PENDING, size: COLUMN_SIZE, status: "PENDING" })
    const preparing = useKitchenTickets({ page: pages.PREPARING, size: COLUMN_SIZE, status: "PREPARING" })
    const ready = useKitchenTickets({ page: pages.READY, size: COLUMN_SIZE, status: "READY" })

    useEffect(() => {
        soundEnabledRef.current = soundEnabled
    }, [soundEnabled])


    useEffect(() => () => {
        highlightTimersRef.current.forEach((timer) => window.clearTimeout(timer))
        void audioContextRef.current?.close()
    }, [])

    const playTicketAlert = useCallback(() => {
        const context = audioContextRef.current
        if (!context) return
        const tones = [1046, 784, 988]
        tones.forEach((frequency, index) => {
            const startAt = context.currentTime + index * 0.42
            const stopAt = startAt + 0.34
            const oscillator = context.createOscillator()
            const gain = context.createGain()
            oscillator.type = "sine"
            oscillator.frequency.setValueAtTime(frequency, startAt)
            gain.gain.setValueAtTime(0.0001, startAt)
            gain.gain.exponentialRampToValueAtTime(0.18, startAt + 0.03)
            gain.gain.exponentialRampToValueAtTime(0.0001, stopAt)
            oscillator.connect(gain)
            gain.connect(context.destination)
            oscillator.start(startAt)
            oscillator.stop(stopAt + 0.02)
        })
    }, [])

    const enableSound = async () => {
        const AudioContextConstructor = window.AudioContext ?? (window as AudioWindow).webkitAudioContext
        if (!AudioContextConstructor) {
            setAudioError("Este navegador no soporta alertas de sonido.")
            return
        }
        try {
            const context = audioContextRef.current ?? new AudioContextConstructor()
            audioContextRef.current = context
            if (context.state === "suspended") {
                await context.resume()
            }
            setAudioError(null)
            setSoundEnabled(true)
            setSoundPromptDismissed(true)
            localStorage.setItem("beet_kds_sound_enabled", "true")
            sessionStorage.setItem(SOUND_PROMPT_KEY, "true")
            playTicketAlert()
        } catch {
            setAudioError("El navegador requiere interaccion para activar sonido.")
        }
    }

    const disableSound = () => {
        setSoundEnabled(false)
        localStorage.setItem("beet_kds_sound_enabled", "false")
    }

    const highlightTicket = useCallback((ticketId: string) => {
        setHighlightedTickets((current) => new Set(current).add(ticketId))
        const existingTimer = highlightTimersRef.current.get(ticketId)
        if (existingTimer) window.clearTimeout(existingTimer)
        const timer = window.setTimeout(() => {
            setHighlightedTickets((current) => {
                const next = new Set(current)
                next.delete(ticketId)
                return next
            })
            highlightTimersRef.current.delete(ticketId)
        }, HIGHLIGHT_MS)
        highlightTimersRef.current.set(ticketId, timer)
    }, [])

    const handleTicketCreated = useCallback((event: KitchenTicketStreamEvent) => {
        highlightTicket(event.ticketId)
        setPages((current) => ({ ...current, PENDING: 0 }))
        if (soundEnabledRef.current) playTicketAlert()
    }, [highlightTicket, playTicketAlert])

    const clearNewTicket = useCallback((ticketId: string) => {
        const timer = highlightTimersRef.current.get(ticketId)
        if (timer) window.clearTimeout(timer)
        highlightTimersRef.current.delete(ticketId)
        setHighlightedTickets((current) => {
            if (!current.has(ticketId)) return current
            const next = new Set(current)
            next.delete(ticketId)
            return next
        })
    }, [])

    const handleTicketEvent = useCallback((event: KitchenTicketStreamEvent) => {
        if (event.eventType === "kitchen.ticket.status_changed") {
            clearNewTicket(event.ticketId)
        }
    }, [clearNewTicket])

    const dismissSoundPrompt = () => {
        setSoundPromptDismissed(true)
        sessionStorage.setItem(SOUND_PROMPT_KEY, "true")
    }

    const stream = useKitchenTicketStream({ onTicketCreated: handleTicketCreated, onTicketEvent: handleTicketEvent })

    const dataByStatus: Record<BoardStatus, ReturnType<typeof useKitchenTickets>> = {
        PENDING: pending,
        PREPARING: preparing,
        READY: ready,
    }

    return (
        <div className="space-y-6 pb-10">
            <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">Live Orders (KDS)</h1>
                    <p className="mt-1 text-muted-foreground">
                        Cocina trabaja por tickets. Cada envio nuevo entra como ticket separado.
                    </p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                    <Badge variant={stream.isConnected ? "default" : "secondary"} className="gap-1">
                        {stream.isConnected ? <Wifi className="h-3.5 w-3.5" /> : <WifiOff className="h-3.5 w-3.5" />}
                        {stream.isConnected ? "Tiempo real activo" : "Reconectando"}
                    </Badge>
                    <div className="relative">
                        {soundEnabled ? (
                            <Button variant="outline" size="sm" onClick={disableSound}>
                                <BellOff className="h-4 w-4" />
                                Silenciar
                            </Button>
                        ) : (
                            <Button variant="outline" size="sm" onClick={enableSound}>
                                <Bell className="h-4 w-4" />
                                Activar sonido
                            </Button>
                        )}
                        {!soundEnabled && !soundPromptDismissed && (
                            <div className="absolute right-0 top-11 z-20 w-72 border bg-background p-3 text-sm shadow-lg">
                                <div className="absolute -top-1 right-6 h-2 w-2 rotate-45 border-l border-t bg-background" />
                                <div className="flex items-start gap-2">
                                    <Bell className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                                    <p className="pr-5 text-muted-foreground">Activa el sonido para recibir alertas de nuevos tickets.</p>
                                    <button
                                        type="button"
                                        className="absolute right-2 top-2 text-muted-foreground hover:text-foreground"
                                        onClick={dismissSoundPrompt}
                                        aria-label="Cerrar recordatorio de sonido"
                                    >
                                        <X className="h-4 w-4" />
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                    {audioError && <p className="text-xs text-destructive">{audioError}</p>}
                </div>
            </div>

            <div className="grid gap-4 xl:grid-cols-3">
                {columns.map((column) => (
                    <TicketColumn
                        key={column.status}
                        title={column.title}
                        description={column.description}
                        status={column.status}
                        query={dataByStatus[column.status]}
                        page={pages[column.status]}
                        now={now}
                        highlightedTickets={highlightedTickets}
                        onPageChange={(page) => setPages((current) => ({ ...current, [column.status]: page }))}
                        onStart={(ticketId) => {
                            clearNewTicket(ticketId)
                            updateStatus.mutate({ ticketId, status: "PREPARING" })
                        }}
                        onReady={(ticketId) => {
                            clearNewTicket(ticketId)
                            updateStatus.mutate({ ticketId, status: "READY" })
                        }}
                        isMutating={updateStatus.isPending}
                    />
                ))}
            </div>
        </div>
    )
}

function TicketColumn({
    title,
    description,
    status,
    query,
    page,
    now,
    highlightedTickets,
    onPageChange,
    onStart,
    onReady,
    isMutating,
}: {
    title: string
    description: string
    status: BoardStatus
    query: ReturnType<typeof useKitchenTickets>
    page: number
    now: number
    highlightedTickets: Set<string>
    onPageChange: (page: number) => void
    onStart: (ticketId: string) => void
    onReady: (ticketId: string) => void
    isMutating: boolean
}) {
    const tickets = query.data?.content ?? []
    return (
        <section className="min-h-[520px] border bg-muted/10">
            <div className="border-b bg-background px-4 py-3">
                <div className="flex items-start justify-between gap-3">
                    <div>
                        <h2 className="font-semibold">{title}</h2>
                        <p className="text-xs text-muted-foreground">{description}</p>
                    </div>
                    <Badge variant="secondary">{query.data?.totalElements ?? 0}</Badge>
                </div>
            </div>
            <div className="space-y-3 p-3">
                {query.isLoading ? (
                    Array.from({ length: 3 }).map((_, index) => (
                        <div key={index} className="h-44 animate-pulse border bg-background" />
                    ))
                ) : tickets.length === 0 ? (
                    <div className="border border-dashed bg-background/70 p-8 text-center text-sm text-muted-foreground">
                        No hay tickets en esta columna.
                    </div>
                ) : tickets.map((ticket) => (
                    <TicketCard
                        key={ticket.id}
                        ticket={ticket}
                        status={status}
                        now={now}
                        isNew={highlightedTickets.has(ticket.id)}
                        isMutating={isMutating}
                        onStart={onStart}
                        onReady={onReady}
                    />
                ))}
            </div>
            {(query.data?.totalPages ?? 0) > 1 && (
                <div className="border-t bg-background px-3 py-2">
                    <ListPagination
                        totalElements={query.data?.totalElements ?? 0}
                        totalPages={query.data?.totalPages ?? 0}
                        page={page}
                        size={COLUMN_SIZE}
                        label="tickets"
                        onPageChange={onPageChange}
                        onSizeChange={() => undefined}
                    />
                </div>
            )}
        </section>
    )
}

function TicketCard({
    ticket,
    status,
    now,
    isNew,
    isMutating,
    onStart,
    onReady,
}: {
    ticket: KitchenTicketResponse
    status: BoardStatus
    now: number
    isNew: boolean
    isMutating: boolean
    onStart: (ticketId: string) => void
    onReady: (ticketId: string) => void
}) {
    const customerName = ticket.customerName?.trim() || "Sin cliente"
    const activeLines = ticket.lines.filter((line) => line.activeQuantity > 0)
    return (
        <article className={cn(
            "border bg-background p-4 transition-all duration-500",
            isNew && "border-primary shadow-lg shadow-primary/15 ring-2 ring-primary/20"
        )}>
            <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                    <p className="flex items-center gap-2 font-semibold">
                        <ChefHat className="h-4 w-4 shrink-0" />
                        <span className="truncate">Orden {ticket.orderDisplayCode ?? ticket.orderId.slice(0, 8)}</span>
                    </p>
                    <p className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
                        <UserRound className="h-3.5 w-3.5" />
                        {customerName}
                    </p>
                    <p className="mt-1 flex items-center gap-1 text-xs text-muted-foreground">
                        <Clock className="h-3.5 w-3.5" />
                        Ticket {ticket.id.slice(0, 8)} - {formatElapsed(ticket.sentAt, now)}
                    </p>
                </div>
                <div className="flex flex-col items-end gap-1">
                    {isNew && <Badge>Nuevo</Badge>}
                    <Badge variant={status === "READY" ? "default" : "secondary"}>{ticket.status}</Badge>
                </div>
            </div>

            <div className="mt-4 space-y-3">
                {activeLines.map((line) => (
                    <div key={line.id} className="border p-3 text-sm">
                        <div className="flex justify-between gap-3">
                            <div className="min-w-0">
                                <p className="font-medium">{line.itemNameSnapshot}</p>
                                {line.notes && <p className="mt-1 text-xs text-muted-foreground">Nota: {line.notes}</p>}
                                {line.canceledQuantity > 0 && (
                                    <p className="mt-1 text-xs text-muted-foreground">
                                        {line.canceledQuantity} cancelada(s)
                                    </p>
                                )}
                            </div>
                            <span className="font-mono font-semibold">x{line.activeQuantity}</span>
                        </div>
                        {line.lineType === "TEMPLATE" && line.templateSlots.length > 0 && (
                            <div className="mt-3 space-y-2 border-t pt-3">
                                {line.templateSlots.map((slot) => (
                                    <div key={slot.id} className="text-xs">
                                        <p className="font-medium text-foreground">{slot.slotNameSnapshot}</p>
                                        {slot.options.length === 0 ? (
                                            <p className="mt-1 text-muted-foreground">Sin selección</p>
                                        ) : (
                                            <ul className="mt-1 space-y-1 text-muted-foreground">
                                                {slot.options.map((option) => (
                                                    <li key={option.id} className="flex justify-between gap-2">
                                                        <span>{option.itemNameSnapshot}</span>
                                                        <span className="font-mono">x{option.quantity}</span>
                                                    </li>
                                                ))}
                                            </ul>
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                ))}
            </div>

            <div className="mt-4 flex justify-end gap-2">
                {status === "PENDING" && (
                    <Button size="sm" variant="outline" disabled={isMutating} onClick={() => onStart(ticket.id)}>
                        <Flame className="h-4 w-4" />
                        Iniciar
                    </Button>
                )}
                {status === "PREPARING" && (
                    <Button size="sm" disabled={isMutating} onClick={() => onReady(ticket.id)}>
                        <Check className="h-4 w-4" />
                        Listo
                    </Button>
                )}
            </div>
        </article>
    )
}

function formatElapsed(value: string, now: number) {
    const timestamp = new Date(value).getTime()
    if (Number.isNaN(timestamp)) return "sin hora"
    const totalSeconds = Math.max(0, Math.floor((now - timestamp) / 1000))
    const minutes = Math.floor(totalSeconds / 60)
    const hours = Math.floor(minutes / 60)
    if (hours > 0) return `${hours}h ${minutes % 60}m`
    if (minutes > 0) return `${minutes}m`
    return "ahora"
}