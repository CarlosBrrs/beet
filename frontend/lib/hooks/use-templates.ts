import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import {
    ApiGenericResponse,
    TemplateResponse,
    CreateTemplateRequest,
    PageResponse,
} from "@/lib/api-types";
import { useRestaurantContext } from "@/components/providers/restaurant-provider";
import { toast } from "sonner";

export const templateKeys = {
    all: ["templates"] as const,
    list: (restaurantId: string | null, params?: { page?: number; size?: number; search?: string }) =>
        [...templateKeys.all, "list", restaurantId, params] as const,
    detail: (restaurantId: string | null, id: string) =>
        [...templateKeys.all, "detail", restaurantId, id] as const,
};

async function request<T>(url: string, init?: { method?: string; body?: string }): Promise<T> {
    const response = await apiClient<ApiGenericResponse<T>>(url, init);
    return response.data;
}

export function useTemplates(params?: { page?: number; size?: number; search?: string }) {
    const { restaurantId } = useRestaurantContext();
    const searchParams = new URLSearchParams();
    if (params?.page !== undefined) searchParams.set("page", String(params.page));
    if (params?.size !== undefined) searchParams.set("size", String(params.size));
    if (params?.search) searchParams.set("search", params.search);
    const queryString = searchParams.toString();
    return useQuery({
        queryKey: templateKeys.list(restaurantId, params),
        queryFn: () => request<PageResponse<TemplateResponse>>(`/restaurants/${restaurantId}/templates${queryString ? `?${queryString}` : ""}`),
        enabled: !!restaurantId,
    });
}

export function useTemplate(templateId?: string | null) {
    const { restaurantId } = useRestaurantContext();
    return useQuery({
        queryKey: templateKeys.detail(restaurantId, templateId ?? ""),
        queryFn: () => request<TemplateResponse>(`/restaurants/${restaurantId}/templates/${templateId}`),
        enabled: !!restaurantId && !!templateId,
    });
}

export function useCreateCatalogTemplate() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();
    return useMutation({
        mutationFn: (payload: CreateTemplateRequest) =>
            request<TemplateResponse>(`/restaurants/${restaurantId}/templates`, {
                method: "POST",
                body: JSON.stringify(payload),
            }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: templateKeys.all });
            toast.success("Plantilla creada exitosamente");
        },
        onError: (error: Error) => toast.error(error.message || "Error al crear la plantilla"),
    });
}

export function useUpdateTemplate(templateId: string) {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();
    return useMutation({
        mutationFn: (payload: CreateTemplateRequest) =>
            request<TemplateResponse>(`/restaurants/${restaurantId}/templates/${templateId}`, {
                method: "PUT",
                body: JSON.stringify(payload),
            }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: templateKeys.all });
            toast.success("Plantilla actualizada exitosamente");
        },
        onError: (error: Error) => toast.error(error.message || "Error al actualizar la plantilla"),
    });
}

export function useSetTemplateActive() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();
    return useMutation({
        mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) =>
            request<TemplateResponse>(`/restaurants/${restaurantId}/templates/${id}/activation`, {
                method: "PATCH",
                body: JSON.stringify({ isActive }),
            }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: templateKeys.all });
            toast.success("Estado de la plantilla actualizado");
        },
        onError: (error: Error) => toast.error(error.message || "No fue posible actualizar la plantilla"),
    });
}
