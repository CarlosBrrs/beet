package com.beet.backend.modules.report.core.application.handler;

import com.beet.backend.modules.report.cash.domain.model.BusinessDayDetailReport;
import com.beet.backend.modules.report.cash.domain.model.BusinessDayReportRow;
import com.beet.backend.modules.report.catalog.domain.model.CatalogReportRow;
import com.beet.backend.modules.report.core.domain.model.ReportGrouping;
import com.beet.backend.modules.report.core.domain.model.ReportOverview;
import com.beet.backend.modules.report.inventory.domain.model.InventoryConsumptionRow;
import com.beet.backend.modules.report.inventory.domain.model.InventoryValuationReport;
import com.beet.backend.modules.report.inventory.domain.model.LowStockReportRow;
import com.beet.backend.modules.report.payments.domain.model.PaymentMethodReportRow;
import com.beet.backend.modules.report.sales.domain.model.SalesSeriesReport;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportHandler {
    ApiGenericResponse<ReportOverview> overview(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to);

    ApiGenericResponse<SalesSeriesReport> salesSeries(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to, ReportGrouping grouping);

    ApiGenericResponse<PageResponse<PaymentMethodReportRow>> paymentMethods(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            int page, int size, String sort, String search);

    ApiGenericResponse<PageResponse<BusinessDayReportRow>> businessDays(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            int page, int size, String sort, String search);

    ApiGenericResponse<BusinessDayDetailReport> businessDayDetail(
            UUID restaurantId, UUID businessDayId);

    ApiGenericResponse<PageResponse<CatalogReportRow>> products(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            int page, int size, String sort, String search);

    ApiGenericResponse<PageResponse<CatalogReportRow>> templates(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            int page, int size, String sort, String search);

    ApiGenericResponse<PageResponse<InventoryConsumptionRow>> inventoryConsumption(
            UUID restaurantId, List<UUID> accountRestaurantIds, LocalDate from, LocalDate to,
            int page, int size, String sort, String search);

    ApiGenericResponse<InventoryValuationReport> inventoryValuation(
            UUID restaurantId, List<UUID> accountRestaurantIds);

    ApiGenericResponse<PageResponse<LowStockReportRow>> lowStock(
            UUID restaurantId, List<UUID> accountRestaurantIds, int page, int size, String sort, String search);
}
