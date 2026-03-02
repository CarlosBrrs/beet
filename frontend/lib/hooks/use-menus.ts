import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import {
    ApiGenericResponse,
    MenuResponse,
    CreateMenuRequest,
    UpdateMenuRequest,
    SubmenuResponse,
    CreateSubmenuRequest,
    UpdateSubmenuRequest
} from "@/lib/api-types";
import { useRestaurantContext } from "@/components/providers/restaurant-provider";
import { toast } from "sonner";

export const menuKeys = {
    all: ["menus"] as const,
    list: (restaurantId: string | null) => [...menuKeys.all, "list", restaurantId] as const,
};

// ── Fetch Functions ──

async function fetchMenus(restaurantId: string): Promise<MenuResponse[]> {
    const data = await apiClient<ApiGenericResponse<MenuResponse[]>>(
        `/restaurants/${restaurantId}/menus`,
        { method: "GET" }
    );
    return data.data;
}

async function createMenuRequest(restaurantId: string, request: CreateMenuRequest): Promise<MenuResponse> {
    const data = await apiClient<ApiGenericResponse<MenuResponse>>(
        `/restaurants/${restaurantId}/menus`,
        { method: "POST", body: JSON.stringify(request) }
    );
    return data.data;
}

async function updateMenuRequest(restaurantId: string, menuId: string, request: UpdateMenuRequest): Promise<MenuResponse> {
    const data = await apiClient<ApiGenericResponse<MenuResponse>>(
        `/restaurants/${restaurantId}/menus/${menuId}`,
        { method: "PUT", body: JSON.stringify(request) }
    );
    return data.data;
}

async function createSubmenuRequest(restaurantId: string, menuId: string, request: CreateSubmenuRequest): Promise<SubmenuResponse> {
    const data = await apiClient<ApiGenericResponse<SubmenuResponse>>(
        `/restaurants/${restaurantId}/menus/${menuId}/submenus`,
        { method: "POST", body: JSON.stringify(request) }
    );
    return data.data;
}

async function updateSubmenuRequest(restaurantId: string, menuId: string, submenuId: string, request: UpdateSubmenuRequest): Promise<SubmenuResponse> {
    const data = await apiClient<ApiGenericResponse<SubmenuResponse>>(
        `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}`,
        { method: "PUT", body: JSON.stringify(request) }
    );
    return data.data;
}

// ── Hooks ──

export function useMenus() {
    const { restaurantId } = useRestaurantContext();

    return useQuery({
        queryKey: menuKeys.list(restaurantId),
        queryFn: () => fetchMenus(restaurantId!),
        enabled: !!restaurantId,
    });
}

export function useCreateMenu() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: (data: CreateMenuRequest) => createMenuRequest(restaurantId!, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: menuKeys.list(restaurantId) });
            toast.success("Menu created successfully");
        },
        onError: (error: any) => {
            toast.error(error.message || "Failed to create menu");
        },
    });
}

export function useUpdateMenu() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: ({ id, data }: { id: string; data: UpdateMenuRequest }) => updateMenuRequest(restaurantId!, id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: menuKeys.list(restaurantId) });
            toast.success("Menu updated successfully");
        },
        onError: (error: any) => {
            toast.error(error.message || "Failed to update menu");
        },
    });
}

export function useCreateSubmenu() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: ({ menuId, data }: { menuId: string; data: CreateSubmenuRequest }) => createSubmenuRequest(restaurantId!, menuId, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: menuKeys.list(restaurantId) });
            toast.success("Submenu created successfully");
        },
        onError: (error: any) => {
            toast.error(error.message || "Failed to create submenu");
        },
    });
}

export function useUpdateSubmenu() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: ({ menuId, id, data }: { menuId: string; id: string; data: UpdateSubmenuRequest }) => updateSubmenuRequest(restaurantId!, menuId, id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: menuKeys.list(restaurantId) });
            toast.success("Submenu updated successfully");
        },
        onError: (error: any) => {
            toast.error(error.message || "Failed to update submenu");
        },
    });
}

