# Beet: Restaurant Management SaaS

[English](#english) | [Español](#español)

---

<a name="english"></a>
## 🇺🇸 English Version

Beet is a comprehensive multi-tenant SaaS platform designed for professional restaurant management. It covers the entire operational lifecycle, from inventory and supplier management to point-of-sale (POS) and advanced financial reporting.

### 🚀 Project Vision
The goal is to provide restaurant owners with a robust tool to manage one or several branches under a single organization, with deep control over costs, stock, and service models.

### 🔑 Core Features

#### 1. Multi-tenant & Organization Management
- **Owner Role**: Absolute control over the organization and its restaurants.
- **Multi-branch**: Manage multiple restaurant locations from a single account.
- **Subscription-based Access**: Feature availability is dictated by the chosen subscription plan.

#### 2. Advanced Inventory & Costing
- **Ingredients & Suppliers**: Track ingredients linked to specific suppliers with multiple units of measure.
- **Base Recipes**: Create reusable recipes that serve as building blocks for products.
- **Dynamic Costing**:
    - **Basic Mode**: Manual general data and pricing (for simpler businesses).
    - **Advanced Mode**: Auto-calculated prices based on ingredient quantities and recipe costs.
- **Real-time Stock**: Automatic deduction of ingredients upon order fulfillment.
- **Stock Alerts & Adjustments**: Low-stock notifications and manual inventory corrections (absolute or relative).

#### 3. Catalog & Menu Hierarchy
- **Structured Menus**: Hierarchy follows `Menu` -> `SubMenu` -> `Product`.
- **Order Templates**: Define configurable templates (e.g., "Build your own bowl") with slots for different proteins or sides, optionality, and specific surcharges.

#### 4. Point of Sale (POS) & Service
- **Service Models**: Supports both "Pay-first" (Counter) and "Consume-first" (Waiter/Table) models.
- **Table Management**: Assign orders to specific tables and manage sections.
- **Order Flow**: Statuses include `Waiting Payment`, `Pending`, `Preparing`, `Ready to Serve`, and `Served`.
- **Comments**: Detailed notes per item and per order.
- **Diners (Comensales)**: Manage multiple diners per table for flexible billing.

#### 5. Financials & Cash Control
- **Register (Caja) Management**: Strict Open/Close procedures.
    - Cannot take orders/payments if the register is closed.
    - Cannot close the register if there are pending orders.
- **Inflow/Outflow**: Track petty cash injections or expenses.
- **Flexible Payments**: Support for partial payments, split bills (by user or percentage), and configurable payment methods (Cash, Credit, Debit, etc.).

#### 6. Role-Based Access Control (RBAC)
- Roles with branch-specific permissions:
    - **Administrator**: Branch Management.
    - **Cashier**: POS & Register control.
    - **Waiter**: Order taking.
    - **Cook / Chef**: Order fulfillment and inventory oversight.

#### 7. Analytics & Reporting
- Comprehensive data engine for:
    - Costs, Sales, and Profit.
    - Performance (Staff, Products, Restaurants).
    - Comparative analysis across branches and time periods.

### 🏗 Technology Stack
- **Backend**: Java 17, Spring Boot 3, Spring Data JDBC, Flyway, PostgreSQL.
- **Architecture**: Hexagonal (Clean Architecture).
- **Communication**: Standardized REST API (`ApiGenericResponse`).
- **DevOps**: Docker & Docker Compose.

---

<a name="español"></a>
## 🇪🇸 Versión en Español

Beet es una plataforma SaaS multi-tenencia integral diseñada para la gestión profesional de restaurantes. Cubre todo el ciclo operativo, desde la gestión de inventarios y proveedores hasta el punto de venta (POS) y reportes financieros avanzados.

### 🚀 Visión del Proyecto
El objetivo es proporcionar a los dueños de restaurantes una herramienta robusta para gestionar una o varias sucursales bajo una sola organización, con un control profundo sobre costos, stock y modelos de servicio.

### 🔑 Características Principales

#### 1. Gestión Multi-tenencia y de Organizaciones
- **Rol de Dueño (Owner)**: Control absoluto sobre la organización y sus restaurantes.
- **Multi-sucursal**: Gestiona múltiples ubicaciones de restaurantes desde una sola cuenta.
- **Acceso por Suscripción**: La disponibilidad de funciones está dictada por el plan de suscripción elegido.

#### 2. Inventario Avanzado y Costeo
- **Ingredientes y Proveedores**: Rastreo de ingredientes vinculados a proveedores específicos con múltiples unidades de medida.
- **Recetas Base**: Creación de recetas reutilizables que sirven como bloques de construcción para los productos.
- **Costeo Dinámico**:
    - **Modo Básico**: Datos generales y precios ingresados manualmente (para negocios simples).
    - **Modo Avanzado**: Precios calculados automáticamente basados en cantidades de ingredientes y costos de recetas.
- **Stock en Tiempo Real**: Deducción automática de ingredientes al completar pedidos.
- **Alertas y Ajustes de Stock**: Notificaciones de bajo stock y correcciones manuales de inventario (absolutas o relativas).

#### 3. Jerarquía de Catálogo y Menús
- **Menús Estructurados**: La jerarquía sigue `Menú` -> `SubMenú` -> `Producto`.
- **Plantillas de Pedido (Templates)**: Define plantillas configurables (ej. "Arma tu bowl") con espacios para diferentes proteínas o acompañamientos, opcionalidad y sobrecostos específicos.

#### 4. Punto de Venta (POS) y Servicio
- **Modelos de Servicio**: Soporta modelos de "Pago primero" (Mostrador) y "Consumo primero" (Mesero/Mesa).
- **Gestión de Mesas**: Asigna pedidos a mesas específicas y gestiona secciones.
- **Flujo de Pedidos**: Estados que incluyen `Esperando Pago`, `Pendiente`, `Preparando`, `Listo para Servir` y `Servido`.
- **Comentarios**: Notas detalladas por ítem y por pedido general.
- **Comensales**: Gestión de múltiples comensales por mesa para facturación flexible.

#### 5. Finanzas y Control de Caja
- **Gestión de Cajas**: Procedimientos estrictos de Apertura/Cierre.
    - No se pueden tomar pedidos/pagos si la caja está cerrada.
    - No se puede cerrar la caja si hay pedidos pendientes.
- **Ingresos/Egresos**: Rastreo de inyecciones de caja menor o gastos.
- **Pagos Flexibles**: Soporte para pagos parciales, cuentas divididas (por usuario o porcentaje) y métodos de pago configurables (Efectivo, Crédito, Débito, etc.).

#### 6. Control de Acceso Basado en Roles (RBAC)
- Roles con permisos específicos por sucursal:
    - **Administrador**: Gestión de sucursal.
    - **Cajero**: Control de POS y caja.
    - **Mesero**: Toma de pedidos.
    - **Cocinero / Chef**: Preparación de pedidos y supervisión de inventario.

#### 7. Analítica y Reportes
- Motor de datos integral para:
    - Costos, Ventas y Ganancias.
    - Rendimiento (Personal, Productos, Restaurantes).
    - Análisis comparativo entre sucursales y periodos de tiempo.

### 🏗 Stack Tecnológico
- **Backend**: Java 17, Spring Boot 3, Spring Data JDBC, Flyway, PostgreSQL.
- **Arquitectura**: Hexagonal (Clean Architecture).
- **Comunicación**: API REST estandarizada (`ApiGenericResponse`).
- **DevOps**: Docker & Docker Compose.

---

## 🛠 Getting Started / Guía de Inicio
- Read [AGENTS.md](AGENTS.md) for architectural standards and project mapping.
- Run locally: `docker-compose up --build`.
