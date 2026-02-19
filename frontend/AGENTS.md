# Frontend Agent Guidelines

This document serves as the **Single Source of Truth** for the Beet Frontend. All agents must strictly adhere to these guidelines.

## 1. Technology Stack

*   **Framework**: Next.js 15 (App Router).
*   **Language**: TypeScript 5+ (Strict Mode).
*   **Styling**: Tailwind CSS 4.
*   **UI Library**: Shadcn UI (Radix Primitives).
*   **Icons**: Lucide React.
*   **State Management**:
    *   **Server State**: TanStack Query (`@tanstack/react-query`) + DevTools.
    *   **Client State (Global)**: React Context (e.g., `AuthProvider`, `RestaurantProvider`).
    *   **Client State (Local)**: `useState` / `useReducer`.
    *   **Forms**: React Hook Form + Zod. **(NO global state for forms)**.
*   **Utilities**: `clsx`, `tailwind-merge` (for class composition).

---

## 2. Project Structure

We follow a **Feature-Based** architecture inside `components/modules` to keep the `app/` directory clean.

```text
frontend/
├── app/                        # Next.js App Router (Routes ONLY)
│   ├── (auth)/                 # Public authentication routes
│   ├── (dashboard)/            # Dashboard routes
│   │   ├── account/            # Global Context (User Profile, Billing)
│   │   ├── admin/              # Global Context (Owner Overview)
│   │   └── restaurants/
│   │       └── [id]/           # Restaurant Context (The "Work" area)
│   ├── api/                    # Route Handlers (Proxy/Edge)
│   ├── layout.tsx              # Root Layout (Providers)
│   └── globals.css             # Tailwind imports & CSS Variables
├── components/
│   ├── ui/                     # Shadcn UI primitives (Button, Input, etc.)
│   ├── modules/                # Domain-specific Feature Modules
│   │   ├── menu/               # e.g., MenuList, MenuForm
│   │   ├── orders/             # e.g., OrderKanban, OrderDetail
│   │   └── restaurants/        # e.g., RestaurantCard, RestaurantSelector
│   ├── shared/                 # Reusable components (Sidebar, Topbar, Can)
│   ├── providers/              # React Context Providers (AuthProvider, QueryProvider)
│   └── theme-provider.tsx      # Next-Themes provider
├── lib/
│   ├── hooks/                  # Custom Hooks (useMenu, usePermissions)
│   ├── api-types.ts            # Shared API Interfaces (No 'any')
│   ├── utils.ts                # Helper functions (cn, formatters)
│   └── env.ts                  # Environment variables
└── .agent/skills/              # Agent Capabilities
```

---

## 3. Architecture & Contexts

The application is vertically sliced into two distinct contexts. **NEVER mix them.**

### A. Global Context (Application Level)
*   **Scope**: User-level management (Billing, Profile, SaaS Subscription, Owner Overview).
*   **Routes**: Any route **NOT** starting with `/restaurants/[id]`.
    *   Examples: `/account`, `/admin/overview`, `/setup`.
*   **Layout**: `layouts/GlobalLayout` (Sidebar: My Restaurants, Billing, Profile).
*   **Data**: User claims from JWT.
*   **Permissions**: **None**. (The concept of RBAC does not exist here).

### B. Restaurant Context (`/restaurants/:id`)
*   **Scope**: Operational management of a specific restaurant.
*   **Base Route**: `/restaurants/[id]/*`
*   **Layout**: `layouts/RestaurantLayout` (Sidebar: Dashboard, POS, Kitchen, Settings).
*   **Configuration**: **MUST** be wrapped in `RestaurantProvider`.
*   **State**: 
    1. Fetches permissions from `GET /restaurants/{id}/my-permissions` via TanStack Query.
    2. Stores `permissions` and `role` in Context.
    3. Exposes `isLoading`, `isError`, and `error` to manage UI states.

### C. Why Separated? (Design Rationale)
*   **Component Safety**: Components like `MenuList` or `OrderKanban` can assume `restaurantId` exists. We avoid adding `if (!restaurantId) return null` boilerplate to hundreds of components.
*   **UX Clarity**: Sidebar links like "Menu" or "Staff" are meaningless without a selected restaurant. Hiding them via a distinct Layout is cleaner than disabling them.
*   **Performance**: We only subscribe to the `permissions` data stream when actually inside a workspace, reducing backend load.

---

## 4. Component & Design Strategy

### Component Strategy
*   **Atomic Design**: Use `components/ui` for atoms (Button, Input).
*   **Module Pattern**: Build complex features in `components/modules/[feature]`.
    *   *Example*: `RestaurantCard` belongs in `modules/restaurants`, not `shared`.
*   **Composition**: Prefer composition over config. Pass `children` instead of big config objects.

### UX Guidelines
*   **Mobile First**: All layouts must be responsive. Use `hidden md:block` strategies.
*   **Feedback**:
    *   **Loading**: Use Skeletons (`<Skeleton />`) for initial loads.
    *   **Mutations**: Use `sonner` (`toast.success()`) for action feedback.
    *   **Errors**: Inline `FormMessage` for forms. Toasts for API errors.
*   **Dark Mode**: Support `dark` and `light` themes via `next-themes`.

---

## 5. Coding Standards

### State Management Rules
1.  **Server Data**: Always use **TanStack Query**. Never use `useEffect` + `fetch`.
    *   Hooks must expose `data`, `isLoading`, `isError`, `error`.
2.  **Forms**: Always use **React Hook Form**.
    *   **NO** Zustand/Redux for form state.
    *   Always use `zod` for schema validation.
3.  **Client Stores**: Use `React Context` for dependency injection (like `RestaurantContext`).
    *   Use **Zustand** ONLY for complex, cross-cutting features (e.g., a Client-Side POS Shopping Cart).

### Type Safety (Strict)
*   **NO `any`**: Usage of `any` is strictly **FORBIDDEN**.
    *   Use `unknown` if truly dynamic, then cast with guards.
    *   Use `Generics` for reusable components/hooks.
*   **Interfaces**: Define interfaces for all props (`Props`) and API responses.
    *   Do not use `I` prefix (e.g., `User`, not `IUser`).

### File Naming
*   **Components**: `kebab-case.tsx` (e.g., `restaurant-card.tsx`).
*   **Hooks**: `use-feature.ts` (e.g., `use-menu.ts`).
*   **Utilities**: `feature-utils.ts`.

---

## 6. Agent Skills

Refer to `.agent/skills` for automated workflows:

| Skill | Description | Usage |
| :--- | :--- | :--- |
| `scaffold_frontend_feature` | Generates List/Form/Page structure. | New CRUD modules |
| `scaffold_react_context` | Generates typed Context implementation. | New Global/Local contexts |
| `scaffold_api_hook` | Generates `useQuery` hooks. | Data fetching |
| `scaffold_guard` | Generates `<Can>` component. | RBAC implementation |
