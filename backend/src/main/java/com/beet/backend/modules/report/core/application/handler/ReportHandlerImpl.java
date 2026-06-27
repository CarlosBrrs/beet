package com.beet.backend.modules.report.core.application.handler;

import com.beet.backend.modules.report.cash.domain.model.BusinessDayDetailReport;
import com.beet.backend.modules.report.cash.domain.model.BusinessDayReportRow;
import com.beet.backend.modules.report.catalog.domain.model.CatalogReportRow;
import com.beet.backend.modules.report.core.domain.api.ReportServicePort;
import com.beet.backend.modules.report.core.domain.model.*;
import com.beet.backend.modules.report.inventory.domain.model.InventoryConsumptionRow;
import com.beet.backend.modules.report.inventory.domain.model.InventoryValuationReport;
import com.beet.backend.modules.report.inventory.domain.model.LowStockReportRow;
import com.beet.backend.modules.report.payments.domain.model.PaymentMethodReportRow;
import com.beet.backend.modules.report.sales.domain.model.SalesSeriesReport;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportHandlerImpl implements ReportHandler {
    private final ReportServicePort service;

    @Override
    public ApiGenericResponse<ReportOverview> overview(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to) {
        return ApiGenericResponse.success(execute(
                "overview", restaurantId, accountRestaurantIds, from, to,
                ReportGrouping.DAY, 0, 20, null, null, null, ReportOverview.class));
    }

    @Override
    public ApiGenericResponse<SalesSeriesReport> salesSeries(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            ReportGrouping grouping) {
        return ApiGenericResponse.success(execute(
                "sales.timeseries", restaurantId, accountRestaurantIds, from, to,
                grouping, 0, 100, null, null, null, SalesSeriesReport.class));
    }

    @Override
    public ApiGenericResponse<PageResponse<PaymentMethodReportRow>> paymentMethods(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            int page, int size, String sort, String search) {
        return ApiGenericResponse.success(page(executePage(
                "payments.methods", restaurantId, accountRestaurantIds, from, to,
                page, size, sort, search)));
    }

    @Override
    public ApiGenericResponse<PageResponse<BusinessDayReportRow>> businessDays(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            int page, int size, String sort, String search) {
        return ApiGenericResponse.success(page(executePage(
                "cash.business-days", restaurantId, accountRestaurantIds, from, to,
                page, size, sort, search)));
    }

    @Override
    public ApiGenericResponse<BusinessDayDetailReport> businessDayDetail(
            UUID restaurantId, UUID businessDayId) {
        return ApiGenericResponse.success(execute(
                "cash.business-day-detail", restaurantId, null, null, null,
                ReportGrouping.DAY, 0, 20, null, null, businessDayId, BusinessDayDetailReport.class));
    }

    @Override
    public ApiGenericResponse<PageResponse<CatalogReportRow>> products(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            int page, int size, String sort, String search) {
        return ApiGenericResponse.success(page(executePage(
                "catalog.products", restaurantId, accountRestaurantIds, from, to,
                page, size, sort, search)));
    }

    @Override
    public ApiGenericResponse<PageResponse<CatalogReportRow>> templates(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            int page, int size, String sort, String search) {
        return ApiGenericResponse.success(page(executePage(
                "catalog.templates", restaurantId, accountRestaurantIds, from, to,
                page, size, sort, search)));
    }

    @Override
    public ApiGenericResponse<PageResponse<InventoryConsumptionRow>> inventoryConsumption(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            int page, int size, String sort, String search) {
        return ApiGenericResponse.success(page(executePage(
                "inventory.consumption", restaurantId, accountRestaurantIds, from, to,
                page, size, sort, search)));
    }

    @Override
    public ApiGenericResponse<InventoryValuationReport> inventoryValuation(
            UUID restaurantId, List<UUID> accountRestaurantIds) {
        return ApiGenericResponse.success(execute(
                "inventory.valuation", restaurantId, accountRestaurantIds, null, null,
                ReportGrouping.DAY, 0, 20, null, null, null, InventoryValuationReport.class));
    }

    @Override
    public ApiGenericResponse<PageResponse<LowStockReportRow>> lowStock(
            UUID restaurantId, List<UUID> accountRestaurantIds, int page, int size,
            String sort, String search) {
        return ApiGenericResponse.success(page(executePage(
                "inventory.low-stock", restaurantId, accountRestaurantIds, null, null,
                page, size, sort, search)));
    }

    private <R extends ReportResult> R execute(
            String key, UUID restaurantId, List<UUID> accountRestaurantIds,
            LocalDate from, LocalDate to, ReportGrouping grouping,
            int page, int size, String sort, String search, UUID resourceId, Class<R> type) {
        ReportContext context = restaurantId == null ? ReportContext.ACCOUNT : ReportContext.RESTAURANT;
        List<UUID> ids = restaurantId == null ? accountRestaurantIds : List.of(restaurantId);
        return service.execute(
                key, context, SecurityUtils.getAuthenticatedUserId(), ids, from, to,
                grouping, page, size, sort, search, resourceId, type);
    }

    @SuppressWarnings("unchecked")
    private <T> ReportPage<T> executePage(
            String key, UUID restaurantId, List<UUID> accountRestaurantIds,
            LocalDate from, LocalDate to, int page, int size, String sort, String search) {
        return (ReportPage<T>) execute(
                key, restaurantId, accountRestaurantIds, from, to,
                ReportGrouping.DAY, page, size, sort, search, null, ReportPage.class);
    }

    private <T> PageResponse<T> page(ReportPage<T> reportPage) {
        return PageResponse.of(
                reportPage.content(), reportPage.totalElements(), reportPage.page(), reportPage.size());
    }
}
