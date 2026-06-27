import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib/api-client";
import {
    ApiGenericResponse,
    ItemResponse,
    CreatePreparationRequest,
    CreateProductRequest,
    UpdateItemRequest,
    PageResponse,
    ProductDependenciesResponse,
} from "@/lib/api-types";
import { useRestaurantContext } from "@/components/providers/restaurant-provider";
import { toast } from "sonner";

// ── Query Keys ──

export const itemKeys = {
    all: ["items"] as const,
    list: (restaurantId: string | null, params?: { itemClass?: string; page?: number; size?: number }) =>
        [...itemKeys.all, "list", restaurantId, params] as const,
    preparations: (restaurantId: string | null) =>
        [...itemKeys.all, "preparations", restaurantId] as const,
    preparation: (restaurantId: string | null, id: string) =>
        [...itemKeys.all, "preparation", restaurantId, id] as const,
    product: (restaurantId: string | null, id: string) =>
        [...itemKeys.all, "product", restaurantId, id] as const,
    templateOptions: (restaurantId: string | null) =>
        [...itemKeys.all, "template-options", restaurantId] as const,
    dependencies: (restaurantId: string | null, id: string) =>
        [...itemKeys.all, "dependencies", restaurantId, id] as const,
};

// ── Fetch Functions ──

async function fetchPreparations(restaurantId: string): Promise<ItemResponse[]> {
    const data = await apiClient<ApiGenericResponse<ItemResponse[]>>(
        `/restaurants/${restaurantId}/preparations`,
        { method: "GET" }
    );
    return data.data;
}

async function fetchItems(
    restaurantId: string,
    params?: { itemClass?: string; page?: number; size?: number; search?: string }
): Promise<PageResponse<ItemResponse>> {
    const searchParams = new URLSearchParams();
    if (params?.itemClass) searchParams.set("itemClass", params.itemClass);
    if (params?.page !== undefined) searchParams.set("page", params.page.toString());
    if (params?.size !== undefined) searchParams.set("size", params.size.toString());
    if (params?.search) searchParams.set("search", params.search);

    const queryString = searchParams.toString();
    const url = `/restaurants/${restaurantId}/products${queryString ? `?${queryString}` : ""}`;
    const data = await apiClient<ApiGenericResponse<PageResponse<ItemResponse>>>(url, { method: "GET" });
    return data.data;
}

async function fetchProductDependencies(
    restaurantId: string,
    productId: string
): Promise<ProductDependenciesResponse> {
    const data = await apiClient<ApiGenericResponse<ProductDependenciesResponse>>(
        `/restaurants/${restaurantId}/products/${productId}/dependencies`,
        { method: "GET" }
    );
    return data.data;
}

async function fetchProduct(restaurantId: string, productId: string): Promise<ItemResponse> {
    const data = await apiClient<ApiGenericResponse<ItemResponse>>(
        `/restaurants/${restaurantId}/products/${productId}`,
        { method: "GET" }
    );
    return data.data;
}

async function fetchTemplateOptions(restaurantId: string): Promise<ItemResponse[]> {
    const data = await apiClient<ApiGenericResponse<ItemResponse[]>>(
        `/restaurants/${restaurantId}/products/template-options`,
        { method: "GET" }
    );
    return data.data;
}

async function createCatalogProductRequest(
    restaurantId: string,
    request: CreateProductRequest
): Promise<ItemResponse> {
    const data = await apiClient<ApiGenericResponse<ItemResponse>>(
        `/restaurants/${restaurantId}/products`,
        { method: "POST", body: JSON.stringify(request) }
    );
    return data.data;
}

async function updateProductRequest(
    restaurantId: string,
    productId: string,
    request: UpdateItemRequest
): Promise<ItemResponse> {
    const data = await apiClient<ApiGenericResponse<ItemResponse>>(
        `/restaurants/${restaurantId}/products/${productId}`,
        { method: "PUT", body: JSON.stringify(request) }
    );
    return data.data;
}

async function setProductActivationRequest(
    restaurantId: string,
    productId: string,
    isActive: boolean
): Promise<ItemResponse> {
    const data = await apiClient<ApiGenericResponse<ItemResponse>>(
        `/restaurants/${restaurantId}/products/${productId}/activation`,
        { method: "PATCH", body: JSON.stringify({ isActive }) }
    );
    return data.data;
}

async function createPreparationRequest(
    restaurantId: string,
    request: CreatePreparationRequest
): Promise<ItemResponse> {
    const data = await apiClient<ApiGenericResponse<ItemResponse>>(
        `/restaurants/${restaurantId}/preparations`,
        { method: "POST", body: JSON.stringify(request) }
    );
    return data.data;
}

async function createProductRequest(
    restaurantId: string,
    menuId: string,
    submenuId: string,
    request: CreateProductRequest
): Promise<ItemResponse> {
    const data = await apiClient<ApiGenericResponse<ItemResponse>>(
        `/restaurants/${restaurantId}/menus/${menuId}/submenus/${submenuId}/products`,
        { method: "POST", body: JSON.stringify(request) }
    );
    return data.data;
}

async function updateItemRequest(
    restaurantId: string,
    itemId: string,
    request: UpdateItemRequest
): Promise<ItemResponse> {
    const data = await apiClient<ApiGenericResponse<ItemResponse>>(
        `/restaurants/${restaurantId}/preparations/${itemId}`,
        { method: "PUT", body: JSON.stringify(request) }
    );
    return data.data;
}

async function deleteItemRequest(restaurantId: string, itemId: string): Promise<void> {
    await apiClient<ApiGenericResponse<void>>(
        `/restaurants/${restaurantId}/preparations/${itemId}`,
        { method: "DELETE" }
    );
}

// ── Hooks ──

export function usePreparations() {
    const { restaurantId } = useRestaurantContext();

    return useQuery({
        queryKey: itemKeys.preparations(restaurantId),
        queryFn: () => fetchPreparations(restaurantId!),
        enabled: !!restaurantId,
    });
}

export function useItems(params?: { itemClass?: string; page?: number; size?: number; search?: string }) {
    const { restaurantId } = useRestaurantContext();
    return useQuery({
        queryKey: itemKeys.list(restaurantId, params),
        queryFn: () => fetchItems(restaurantId!, params),
        enabled: !!restaurantId,
    });
}

export function useProducts(params?: { page?: number; size?: number; search?: string }) {
    return useItems({ ...params, itemClass: "PRODUCT" });
}

export function useProductById(productId?: string | null) {
    const { restaurantId } = useRestaurantContext();
    return useQuery({
        queryKey: itemKeys.product(restaurantId, productId ?? ""),
        queryFn: () => fetchProduct(restaurantId!, productId!),
        enabled: !!restaurantId && !!productId,
    });
}

export function useProductDependencies(productId?: string | null) {
    const { restaurantId } = useRestaurantContext();
    return useQuery({
        queryKey: itemKeys.dependencies(restaurantId, productId ?? ""),
        queryFn: () => fetchProductDependencies(restaurantId!, productId!),
        enabled: !!restaurantId && !!productId,
    });
}

export function useTemplateOptionProducts() {
    const { restaurantId } = useRestaurantContext();
    return useQuery({
        queryKey: itemKeys.templateOptions(restaurantId),
        queryFn: () => fetchTemplateOptions(restaurantId!),
        enabled: !!restaurantId,
    });
}

export function useCreateCatalogProduct() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();
    return useMutation({
        mutationFn: (data: CreateProductRequest) => createCatalogProductRequest(restaurantId!, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: itemKeys.all });
            toast.success("Producto creado exitosamente");
        },
        onError: (error: Error) => toast.error(error.message || "Error al crear el producto"),
    });
}

export function useUpdateProduct() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();
    return useMutation({
        mutationFn: ({ id, data }: { id: string; data: UpdateItemRequest }) =>
            updateProductRequest(restaurantId!, id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: itemKeys.all });
            toast.success("Producto actualizado exitosamente");
        },
        onError: (error: Error) => toast.error(error.message || "Error al actualizar el producto"),
    });
}

export function useSetProductActive() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();
    return useMutation({
        mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) =>
            setProductActivationRequest(restaurantId!, id, isActive),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: itemKeys.all });
            toast.success("Estado del producto actualizado");
        },
        onError: (error: Error) => toast.error(error.message || "No fue posible actualizar el producto"),
    });
}

export function useCreatePreparation() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: (data: CreatePreparationRequest) =>
            createPreparationRequest(restaurantId!, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: itemKeys.all });
            toast.success("Preparación creada exitosamente");
        },
        onError: (error: Error) => {
            toast.error(error.message || "Error al crear la preparación");
        },
    });
}

export function useCreateProduct() {
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
            data: CreateProductRequest;
        }) => createProductRequest(restaurantId!, menuId, submenuId, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: itemKeys.preparations(restaurantId) });
            toast.success("Producto creado exitosamente");
        },
        onError: (error: Error) => {
            toast.error(error.message || "Error al crear el producto");
        },
    });
}

export function useUpdateItem() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: ({ id, data }: { id: string; data: UpdateItemRequest }) =>
            updateItemRequest(restaurantId!, id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: itemKeys.preparations(restaurantId) });
            toast.success("Item actualizado exitosamente");
        },
        onError: (error: Error) => {
            toast.error(error.message || "Error al actualizar el item");
        },
    });
}

export function useDeleteItem() {
    const queryClient = useQueryClient();
    const { restaurantId } = useRestaurantContext();

    return useMutation({
        mutationFn: (id: string) => deleteItemRequest(restaurantId!, id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: itemKeys.preparations(restaurantId) });
            toast.success("Item eliminado exitosamente");
        },
        onError: (error: Error) => {
            toast.error(error.message || "Error al eliminar el item");
        },
    });
}
