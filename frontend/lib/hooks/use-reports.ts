"use client"

import { useQuery } from "@tanstack/react-query"

import { useOptionalRestaurantContext } from "@/components/providers/restaurant-provider"
import {
    ApiGenericResponse,
    BusinessDayDetailReport,
    BusinessDayReportRow,
    CatalogReportRow,
    InventoryConsumptionReportRow,
    InventoryValuationReport,
    LowStockReportRow,
    PageResponse,
    PaymentMethodReportRow,
    ReportOverview,
    SalesSeriesReport,
} from "@/lib/api-types"
import { apiClient } from "@/lib/api-client"

export type ReportGrouping = "DAY" | "WEEK" | "MONTH"

export interface ReportFilters {
    dateFrom: string
    dateTo: string
    restaurantIds?: string[]
    grouping?: ReportGrouping
    page?: number
    size?: number
    sort?: string
    search?: string
}

export const reportKeys = {
    all: ["reports"] as const,
    query: (scope: string, resource: string, filters: ReportFilters) =>
        [...reportKeys.all, scope, resource, filters] as const,
}

function queryString(filters: ReportFilters, includeDates = true) {
    const params = new URLSearchParams()
    if (includeDates) {
        params.set("dateFrom", filters.dateFrom)
        params.set("dateTo", filters.dateTo)
    }
    filters.restaurantIds?.forEach((id) => params.append("restaurantId", id))
    if (filters.grouping) params.set("grouping", filters.grouping)
    if (filters.page !== undefined) params.set("page", String(filters.page))
    if (filters.size !== undefined) params.set("size", String(filters.size))
    if (filters.sort) params.set("sort", filters.sort)
    if (filters.search) params.set("search", filters.search)
    return params.toString()
}

function useReportEndpoint<T>(
    resource: string,
    filters: ReportFilters,
    enabled = true,
    includeDates = true
) {
    const restaurant = useOptionalRestaurantContext()
    const base = restaurant?.restaurantId
        ? `/restaurants/${restaurant.restaurantId}/reports`
        : "/account/reports"
    const scope = restaurant?.restaurantId ?? "account"
    return useQuery({
        queryKey: reportKeys.query(scope, resource, filters),
        queryFn: async () => {
            const suffix = queryString(filters, includeDates)
            const response = await apiClient<ApiGenericResponse<T>>(
                `${base}/${resource}${suffix ? `?${suffix}` : ""}`
            )
            return response.data
        },
        enabled: enabled && (!!restaurant?.restaurantId || filters.restaurantIds === undefined || filters.restaurantIds.length > 0),
    })
}

export function useReportOverview(filters: ReportFilters, enabled = true) {
    return useReportEndpoint<ReportOverview>("overview", filters, enabled)
}

export function useSalesSeries(filters: ReportFilters, enabled = true) {
    return useReportEndpoint<SalesSeriesReport>("sales/timeseries", filters, enabled)
}

export function usePaymentMethodReport(filters: ReportFilters, enabled = true) {
    return useReportEndpoint<PageResponse<PaymentMethodReportRow>>("payment-methods", filters, enabled)
}

export function useBusinessDayReport(filters: ReportFilters, enabled = true) {
    return useReportEndpoint<PageResponse<BusinessDayReportRow>>("business-days", filters, enabled)
}

export function useBusinessDayDetailReport(businessDayId: string | null, enabled = true) {
    const restaurant = useOptionalRestaurantContext()
    return useQuery({
        queryKey: [...reportKeys.all, restaurant?.restaurantId, "business-day-detail", businessDayId],
        queryFn: async () => {
            const response = await apiClient<ApiGenericResponse<BusinessDayDetailReport>>(
                `/restaurants/${restaurant!.restaurantId}/reports/business-days/${businessDayId}`
            )
            return response.data
        },
        enabled: enabled && !!restaurant?.restaurantId && !!businessDayId,
    })
}

export function useProductReport(filters: ReportFilters, enabled = true) {
    return useReportEndpoint<PageResponse<CatalogReportRow>>("products", filters, enabled)
}

export function useTemplateReport(filters: ReportFilters, enabled = true) {
    return useReportEndpoint<PageResponse<CatalogReportRow>>("templates", filters, enabled)
}

export function useInventoryConsumptionReport(filters: ReportFilters, enabled = true) {
    return useReportEndpoint<PageResponse<InventoryConsumptionReportRow>>(
        "inventory/consumption", filters, enabled
    )
}

export function useInventoryValuationReport(filters: ReportFilters, enabled = true) {
    return useReportEndpoint<InventoryValuationReport>("inventory/valuation", filters, enabled, false)
}

export function useLowStockReport(filters: ReportFilters, enabled = true) {
    return useReportEndpoint<PageResponse<LowStockReportRow>>(
        "inventory/low-stock", filters, enabled, false
    )
}
