"use client"

import { ClipboardList, ChevronDown } from "lucide-react"

export function CatalogRoadmapNote() {
    return (
        <details className="group max-w-5xl border bg-muted/20">
            <summary className="flex cursor-pointer list-none items-center justify-between gap-3 px-4 py-3 text-sm font-medium">
                <span className="flex items-center gap-2">
                    <ClipboardList className="h-4 w-4 text-muted-foreground" />
                    Recordatorios de evolucion del catalogo
                </span>
                <ChevronDown className="h-4 w-4 text-muted-foreground transition-transform group-open:rotate-180" />
            </summary>
            <div className="grid gap-5 border-t px-4 py-4 text-xs leading-relaxed text-muted-foreground lg:grid-cols-2">
                <section>
                    <h3 className="mb-2 font-semibold text-foreground">Limites actuales</h3>
                    <ul className="list-disc space-y-1 pl-4">
                        <li>Cada producto o armable puede publicarse individualmente en un solo submenu.</li>
                        <li>Un producto puede reutilizarse como opcion dentro de multiples armables.</li>
                        <li>Los armables no pueden contener otros armables.</li>
                        <li>Las preparaciones son internas: no se publican ni se seleccionan directamente en slots.</li>
                    </ul>
                </section>
                <section>
                    <h3 className="mb-2 font-semibold text-foreground">Pendientes del editor de armables</h3>
                    <ul className="list-disc space-y-1 pl-4">
                        <li>Agregar busqueda y paginacion al selector interno de opciones.</li>
                        <li>Agregar reordenamiento visual de slots y opciones.</li>
                        <li>Mostrar en el hub el submenu donde se publico cada armable.</li>
                        <li>Crear una vista de detalle separada del formulario de edicion.</li>
                    </ul>
                </section>
                <section>
                    <h3 className="mb-2 font-semibold text-foreground">Pendientes de productos</h3>
                    <ul className="list-disc space-y-1 pl-4">
                        <li>Agregar filtros y ordenamiento por publicacion, actividad, tipo y uso comercial.</li>
                        <li>Mejorar la presentacion visual de productos, recetas y composicion.</li>
                        <li>Mostrar menu y submenu publicados directamente en el hub.</li>
                        <li>Detallar dependencias tambien al retirar elegibilidad para armables.</li>
                        <li>
                            Crear un flujo explicito para convertir productos planos existentes en rastreados,
                            agregando receta, rendimiento y modelo de costo sin perder sus publicaciones ni usos en armables.
                        </li>
                    </ul>
                </section>
                <section>
                    <h3 className="mb-2 font-semibold text-foreground">Pendientes de menus y submenus</h3>
                    <ul className="list-disc space-y-1 pl-4">
                        <li>Simplificar el acceso directo a la gestion de elementos de cada submenu.</li>
                        <li>Evitar depender del recorrido Menu, visualizacion o edicion, submenu y gestion de elementos.</li>
                        <li>Mejorar la visualizacion de estructura, productos publicados y orden comercial.</li>
                        <li>Agregar filtros y reordenamiento persistente de elementos publicados.</li>
                    </ul>
                </section>
                <section>
                    <h3 className="mb-2 font-semibold text-foreground">Pendiente: menu publico para clientes</h3>
                    <ul className="list-disc space-y-1 pl-4">
                        <li>Entregar una URL publica estable por restaurante para consultar la oferta publicada.</li>
                        <li>Mostrar menus, submenus y productos habilitados con su informacion comercial.</li>
                        <li>Permitir que un usuario autorizado regenere la version publica cuando decida refrescar el contenido.</li>
                        <li>Generar un PDF descargable a partir de la misma version publicada del menu.</li>
                        <li>Separar los cambios administrativos en curso de la ultima version visible para clientes.</li>
                    </ul>
                </section>
            </div>
        </details>
    )
}
