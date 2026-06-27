"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import {
    AcceptStaffInvitationRequest,
    ApiGenericResponse,
    PageResponse,
    PermissionCatalogResponse,
    StaffAssignmentRequest,
    StaffInvitationRequest,
    StaffInvitationResponse,
    StaffMemberResponse,
    StaffRoleResponse,
} from "@/lib/api-types"

const staffKeys = {
    all: ["staff"] as const,
    restaurant: (restaurantId: string) => [...staffKeys.all, "restaurant", restaurantId] as const,
    roles: (restaurantId: string) => [...staffKeys.restaurant(restaurantId), "roles"] as const,
    catalog: (restaurantId: string) => [...staffKeys.restaurant(restaurantId), "catalog"] as const,
    employees: (restaurantId: string, params: StaffListParams) => [...staffKeys.restaurant(restaurantId), "employees", params] as const,
    invitations: (restaurantId: string, params: StaffListParams) => [...staffKeys.restaurant(restaurantId), "invitations", params] as const,
    account: (params: StaffListParams) => [...staffKeys.all, "account", params] as const,
    invitation: (token: string) => [...staffKeys.all, "invitation", token] as const,
}

export interface StaffListParams {
    page?: number
    size?: number
    search?: string
    status?: string
    roleId?: string
    restaurantId?: string
}

function queryString(params: StaffListParams = {}) {
    const search = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") search.set(key, String(value))
    })
    const value = search.toString()
    return value ? `?${value}` : ""
}

function unwrap<T>(response: ApiGenericResponse<T>): T {
    return response.data
}

export function usePermissionCatalog(restaurantId: string) {
    return useQuery({
        queryKey: staffKeys.catalog(restaurantId),
        queryFn: async () => unwrap(await apiClient<ApiGenericResponse<PermissionCatalogResponse[]>>(
            `/restaurants/${restaurantId}/staff/permission-catalog`
        )),
        enabled: !!restaurantId,
    })
}

export function useStaffRoles(restaurantId: string) {
    return useQuery({
        queryKey: staffKeys.roles(restaurantId),
        queryFn: async () => unwrap(await apiClient<ApiGenericResponse<StaffRoleResponse[]>>(
            `/restaurants/${restaurantId}/staff/roles`
        )),
        enabled: !!restaurantId,
    })
}

export function useRestaurantStaff(restaurantId: string, params: StaffListParams) {
    return useQuery({
        queryKey: staffKeys.employees(restaurantId, params),
        queryFn: async () => unwrap(await apiClient<ApiGenericResponse<PageResponse<StaffMemberResponse>>>(
            `/restaurants/${restaurantId}/staff${queryString(params)}`
        )),
        enabled: !!restaurantId,
    })
}

export function useStaffInvitations(restaurantId: string, params: StaffListParams) {
    return useQuery({
        queryKey: staffKeys.invitations(restaurantId, params),
        queryFn: async () => unwrap(await apiClient<ApiGenericResponse<PageResponse<StaffInvitationResponse>>>(
            `/restaurants/${restaurantId}/staff/invitations${queryString(params)}`
        )),
        enabled: !!restaurantId,
    })
}

export function useAccountStaff(params: StaffListParams) {
    return useQuery({
        queryKey: staffKeys.account(params),
        queryFn: async () => unwrap(await apiClient<ApiGenericResponse<PageResponse<StaffMemberResponse>>>(
            `/account/staff${queryString(params)}`
        )),
    })
}

export function useInvitationPreview(token: string) {
    return useQuery({
        queryKey: staffKeys.invitation(token),
        queryFn: async () => unwrap(await apiClient<ApiGenericResponse<StaffInvitationResponse>>(
            `/staff-invitations/${token}`
        )),
        enabled: !!token,
        retry: 0,
    })
}

export function useAcceptInvitation(token: string) {
    return useMutation({
        mutationFn: async (payload: AcceptStaffInvitationRequest) => unwrap(
            await apiClient<ApiGenericResponse<StaffMemberResponse>>(`/staff-invitations/${token}/accept`, {
                method: "POST",
                body: JSON.stringify(payload),
            })
        ),
    })
}

export function useStaffMutations(restaurantId: string) {
    const queryClient = useQueryClient()
    const invalidate = async () => {
        await queryClient.invalidateQueries({ queryKey: staffKeys.restaurant(restaurantId) })
        await queryClient.invalidateQueries({ queryKey: ["my-permissions"] })
    }

    return {
        invite: useMutation({
            mutationFn: async (payload: StaffInvitationRequest) => unwrap(
                await apiClient<ApiGenericResponse<StaffInvitationResponse>>(
                    `/restaurants/${restaurantId}/staff/invitations`,
                    { method: "POST", body: JSON.stringify(payload) }
                )
            ),
            onSuccess: invalidate,
        }),
        regenerateInvitation: useMutation({
            mutationFn: async (invitationId: string) => unwrap(
                await apiClient<ApiGenericResponse<StaffInvitationResponse>>(
                    `/restaurants/${restaurantId}/staff/invitations/${invitationId}/regenerate`,
                    { method: "POST" }
                )
            ),
            onSuccess: invalidate,
        }),
        revokeInvitation: useMutation({
            mutationFn: async ({ invitationId, reason }: { invitationId: string; reason?: string }) => unwrap(
                await apiClient<ApiGenericResponse<StaffInvitationResponse>>(
                    `/restaurants/${restaurantId}/staff/invitations/${invitationId}/revoke`,
                    { method: "POST", body: JSON.stringify({ reason }) }
                )
            ),
            onSuccess: invalidate,
        }),
        updateAssignment: useMutation({
            mutationFn: async ({ userId, payload }: { userId: string; payload: StaffAssignmentRequest }) => unwrap(
                await apiClient<ApiGenericResponse<StaffMemberResponse>>(`/restaurants/${restaurantId}/staff/${userId}`, {
                    method: "PATCH",
                    body: JSON.stringify(payload),
                })
            ),
            onSuccess: invalidate,
        }),
        removeAssignment: useMutation({
            mutationFn: async (userId: string) => unwrap(
                await apiClient<ApiGenericResponse<void>>(`/restaurants/${restaurantId}/staff/${userId}`, {
                    method: "DELETE",
                })
            ),
            onSuccess: invalidate,
        }),
    }
}

export function useAccountStaffMutations() {
    const queryClient = useQueryClient()
    return {
        updateAccountStatus: useMutation({
            mutationFn: async ({ userId, status }: { userId: string; status: "ACTIVE" | "SUSPENDED" }) => unwrap(
                await apiClient<ApiGenericResponse<StaffMemberResponse>>(`/account/staff/${userId}/status`, {
                    method: "PATCH",
                    body: JSON.stringify({ status }),
                })
            ),
            onSuccess: async () => {
                await queryClient.invalidateQueries({ queryKey: staffKeys.all })
                await queryClient.invalidateQueries({ queryKey: ["my-permissions"] })
            },
        }),
    }
}