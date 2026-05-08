import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import {
    ApiGenericResponse,
    TemplateResponse,
    CreateTemplateRequest,
} from "@/lib/api-types";
import { useRestaurantContext } from "@/components/providers/restaurant-provider";
import { toast } from "sonner";

// ── Query Keys ──

export const templateKeys = {
    all: ["templates"] as const,
    detail: (id: string) => [...templateKeys.all, "detail", id] as const,
};

// ── Fetch Functions ──

async function createTemplateRequest(
    restaurantId: string,
    menuId: string,
    submenuId: string,
    request: CreateTemplateRequest
): Promise<TemplateResponse> {
    const data = await apiClient<ApiGenericResponse<TemplateResponse>>(
        `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/templates`,
        { method: "POST", body: JSON.stringify(request) }
    );
    return data.data;
}

// ── Hooks ──

export function useCreateTemplate() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: ({
            menuId,
            submenuId,
            data,
        }: {
            menuId: string;
            submenuId: string;
            data: CreateTemplateRequest;
        }) => createTemplateRequest(restaurantId!, menuId, submenuId, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: templateKeys.all });
            toast.success("Template creado exitosamente");
        },
        onError: (error: any) => {
            toast.error(error.message || "Error al crear el template");
        },
    });
}
