package com.beet.backend.modules.report.core.infrastructure.input.rest;

import com.beet.backend.modules.report.cash.domain.model.BusinessDayReportRow;
import com.beet.backend.modules.report.catalog.domain.model.CatalogReportRow;
import com.beet.backend.modules.report.core.application.handler.ReportHandler;
import com.beet.backend.modules.report.core.domain.model.ReportGrouping;
import com.beet.backend.modules.report.core.domain.model.ReportOverview;
import com.beet.backend.modules.report.inventory.domain.model.InventoryConsumptionRow;
import com.beet.backend.modules.report.inventory.domain.model.InventoryValuationReport;
import com.beet.backend.modules.report.inventory.domain.model.LowStockReportRow;
import com.beet.backend.modules.report.payments.domain.model.PaymentMethodReportRow;
import com.beet.backend.modules.report.sales.domain.model.SalesSeriesReport;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/account/reports")
@RequiredArgsConstructor
public class AccountReportController {
    private final ReportHandler handler;

    @GetMapping("/overview")
    public ResponseEntity<ApiGenericResponse<ReportOverview>> overview(
            @RequestParam(required = false) List<UUID> restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(handler.overview(null, restaurantId, dateFrom, dateTo));
    }

    @GetMapping("/sales/timeseries")
    public ResponseEntity<ApiGenericResponse<SalesSeriesReport>> salesTimeseries(
            @RequestParam(required = false) List<UUID> restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "DAY") ReportGrouping grouping) {
        return ResponseEntity.ok(handler.salesSeries(null, restaurantId, dateFrom, dateTo, grouping));
    }

    @GetMapping("/payment-methods")
    public ResponseEntity<ApiGenericResponse<PageResponse<PaymentMethodReportRow>>> paymentMethods(
            @RequestParam(required = false) List<UUID> restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.paymentMethods(
                null, restaurantId, dateFrom, dateTo, page, size, sort, search));
    }

    @GetMapping("/business-days")
    public ResponseEntity<ApiGenericResponse<PageResponse<BusinessDayReportRow>>> businessDays(
            @RequestParam(required = false) List<UUID> restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.businessDays(
                null, restaurantId, dateFrom, dateTo, page, size, sort, search));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiGenericResponse<PageResponse<CatalogReportRow>>> products(
            @RequestParam(required = false) List<UUID> restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.products(
                null, restaurantId, dateFrom, dateTo, page, size, sort, search));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiGenericResponse<PageResponse<CatalogReportRow>>> templates(
            @RequestParam(required = false) List<UUID> restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.templates(
                null, restaurantId, dateFrom, dateTo, page, size, sort, search));
    }

    @GetMapping("/inventory/consumption")
    public ResponseEntity<ApiGenericResponse<PageResponse<InventoryConsumptionRow>>> inventoryConsumption(
            @RequestParam(required = false) List<UUID> restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.inventoryConsumption(
                null, restaurantId, dateFrom, dateTo, page, size, sort, search));
    }

    @GetMapping("/inventory/valuation")
    public ResponseEntity<ApiGenericResponse<InventoryValuationReport>> inventoryValuation(
            @RequestParam(required = false) List<UUID> restaurantId) {
        return ResponseEntity.ok(handler.inventoryValuation(null, restaurantId));
    }

    @GetMapping("/inventory/low-stock")
    public ResponseEntity<ApiGenericResponse<PageResponse<LowStockReportRow>>> lowStock(
            @RequestParam(required = false) List<UUID> restaurantId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.lowStock(null, restaurantId, page, size, sort, search));
    }
}
