import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import {
    ApiGenericResponse,
    CreateProductRequest,
    CreateTemplateRequest,
    SubmenuNodeResponse,
    ItemResponse,
    UpdateItemRequest,
} from "@/lib/api-types";
import { useRestaurantContext } from "@/components/providers/restaurant-provider";
import { toast } from "sonner";

// ── Query Keys ──
export const submenuNodeKeys = {
    all: ["submenu-nodes"] as const,
    list: (restaurantId: string | null, menuId: string, submenuId: string) =>
        [...submenuNodeKeys.all, restaurantId, menuId, submenuId] as const,
    item: (restaurantId: string | null, menuId: string, submenuId: string, productId: string) =>
        [...submenuNodeKeys.all, restaurantId, menuId, submenuId, "products", productId] as const,
};

// ── Hooks ──

export function useSubmenuNodes(menuId: string, submenuId: string) {
    const { restaurantId } = useRestaurantContext();

    return useQuery({
        queryKey: submenuNodeKeys.list(restaurantId, menuId, submenuId),
        queryFn: async () => {
            // Note: Currently backend `getSubmenuNodes` returns `List<ItemResponse>`.
            // We map this raw array into the polymorphic `SubmenuNodeResponse` expected by the UI.
            const data = await apiClient<ApiGenericResponse<any[]>>(
                `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/nodes`,
                { method: "GET" }
            );
            
            return data.data.map((item: any) => ({
                id: item.id,
                submenuId: submenuId,
                nodeType: "PRODUCT" as const,
                itemId: item.id,
                templateId: null,
                sortOrder: 0, // Fallback until backend supports ordering
                item: item
            })) as SubmenuNodeResponse[];
        },
        enabled: !!restaurantId && !!menuId && !!submenuId,
    });
}

export function useProduct(menuId: string | undefined, submenuId: string | undefined, productId: string | undefined) {
    const { restaurantId } = useRestaurantContext();

    return useQuery({
        queryKey: submenuNodeKeys.item(restaurantId, menuId || "", submenuId || "", productId || ""),
        queryFn: async () => {
            const data = await apiClient<ApiGenericResponse<ItemResponse>>(
                `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/products/${productId}`,
                { method: "GET" }
            );
            return data.data;
        },
        enabled: !!restaurantId && !!menuId && !!submenuId && !!productId,
    });
}

export function useCreateSubmenuProduct(menuId: string, submenuId: string) {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: async (payload: CreateProductRequest) => {
            const data = await apiClient<ApiGenericResponse<SubmenuNodeResponse>>(
                `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/products`,
                { method: "POST", body: JSON.stringify(payload) }
            );
            return data.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: submenuNodeKeys.list(restaurantId, menuId, submenuId),
            });
            toast.success("Producto creado exitosamente");
        },
        onError: (error: any) => {
            toast.error(error.message || "Error al crear producto");
        },
    });
}

export function useUpdateSubmenuProduct(menuId: string, submenuId: string, productId: string) {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: async (payload: import("@/lib/api-types").UpdateItemRequest) => {
            const data = await apiClient<ApiGenericResponse<ItemResponse>>(
                `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/products/${productId}`,
                { method: "PUT", body: JSON.stringify(payload) }
            );
            return data.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: submenuNodeKeys.list(restaurantId, menuId, submenuId),
            });
            queryClient.invalidateQueries({
                queryKey: submenuNodeKeys.item(restaurantId, menuId, submenuId, productId),
            });
            toast.success("Producto actualizado exitosamente");
        },
        onError: (error: any) => {
            toast.error(error.message || "Error al actualizar producto");
        },
    });
}

export function useCreateSubmenuTemplate(menuId: string, submenuId: string) {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: async (payload: CreateTemplateRequest) => {
            const data = await apiClient<ApiGenericResponse<SubmenuNodeResponse>>(
                `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/templates`,
                { method: "POST", body: JSON.stringify(payload) }
            );
            return data.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: submenuNodeKeys.list(restaurantId, menuId, submenuId),
            });
            toast.success("Plantilla creada exitosamente");
        },
        onError: (error: any) => {
            toast.error(error.message || "Error al crear plantilla");
        },
    });
}

export function useDeleteSubmenuNode(menuId: string, submenuId: string) {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: async (nodeId: string) => {
            await apiClient<ApiGenericResponse<void>>(
                `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/nodes/${nodeId}`,
                { method: "DELETE" }
            );
        },
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: submenuNodeKeys.list(restaurantId, menuId, submenuId),
            });
            toast.success("Elemento removido del submenú");
        },
        onError: (error: any) => {
            toast.error(error.message || "Error al eliminar");
        },
    });
}
