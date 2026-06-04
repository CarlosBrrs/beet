import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import {
    ApiGenericResponse,
    CreateProductRequest,
    CreateTemplateRequest,
    SubmenuNodeResponse,
    ItemResponse,
    UpdateItemRequest,
    TemplateResponse,
    PublishSubmenuNodeRequest,
} from "@/lib/api-types";
import { itemKeys } from "@/lib/hooks/use-items";
import { templateKeys } from "@/lib/hooks/use-templates";
import { useRestaurantContext } from "@/components/providers/restaurant-provider";
import { toast } from "sonner";

function getErrorMessage(error: unknown, fallback: string) {
    return error instanceof Error ? error.message : fallback;
}

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
            const data = await apiClient<ApiGenericResponse<SubmenuNodeResponse[]>>(
                `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/nodes`,
                { method: "GET" }
            );

            return data.data;
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
            const data = await apiClient<ApiGenericResponse<ItemResponse>>(
                `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/products`,
                { method: "POST", body: JSON.stringify(payload) }
            );
            return data.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: submenuNodeKeys.list(restaurantId, menuId, submenuId),
            });
            queryClient.invalidateQueries({ queryKey: itemKeys.all });
            toast.success("Producto creado exitosamente");
        },
        onError: (error: unknown) => {
            toast.error(getErrorMessage(error, "Error al crear producto"));
        },
    });
}

export function useUpdateSubmenuProduct(menuId: string, submenuId: string, productId: string) {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: async (payload: UpdateItemRequest) => {
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
            queryClient.invalidateQueries({ queryKey: itemKeys.all });
            toast.success("Producto actualizado exitosamente");
        },
        onError: (error: unknown) => {
            toast.error(getErrorMessage(error, "Error al actualizar producto"));
        },
    });
}

export function useCreateSubmenuTemplate(menuId: string, submenuId: string) {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: async (payload: CreateTemplateRequest) => {
            const data = await apiClient<ApiGenericResponse<TemplateResponse>>(
                `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/templates`,
                { method: "POST", body: JSON.stringify(payload) }
            );
            return data.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: submenuNodeKeys.list(restaurantId, menuId, submenuId),
            });
            queryClient.invalidateQueries({ queryKey: templateKeys.all });
            toast.success("Plantilla creada exitosamente");
        },
        onError: (error: unknown) => {
            toast.error(getErrorMessage(error, "Error al crear plantilla"));
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
            queryClient.invalidateQueries({ queryKey: itemKeys.all });
            queryClient.invalidateQueries({ queryKey: templateKeys.all });
            toast.success("Elemento removido del submenú");
        },
        onError: (error: unknown) => {
            toast.error(getErrorMessage(error, "Error al eliminar"));
        },
    });
}

export function usePublishSubmenuNode(menuId: string, submenuId: string) {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: async (payload: PublishSubmenuNodeRequest) => {
            const data = await apiClient<ApiGenericResponse<SubmenuNodeResponse>>(
                `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/nodes`,
                { method: "POST", body: JSON.stringify(payload) }
            );
            return data.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: submenuNodeKeys.list(restaurantId, menuId, submenuId) });
            queryClient.invalidateQueries({ queryKey: itemKeys.all });
            queryClient.invalidateQueries({ queryKey: templateKeys.all });
            toast.success("Elemento publicado en el submenu");
        },
        onError: (error: unknown) => toast.error(getErrorMessage(error, "Error al publicar el elemento")),
    });
}
