package com.beet.backend.modules.report.core.infrastructure.input.rest;

import com.beet.backend.modules.report.cash.domain.model.BusinessDayDetailReport;
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
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.PermissionRequirement;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants/{restaurantId}/reports")
@RequiredArgsConstructor
public class RestaurantReportController {
    private final ReportHandler handler;

    @GetMapping("/overview")
    @RequiresPermission(module = PermissionModule.FINANCE, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<ReportOverview>> overview(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(handler.overview(restaurantId, null, dateFrom, dateTo));
    }

    @GetMapping("/sales/timeseries")
    @RequiresPermission(module = PermissionModule.FINANCE, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<SalesSeriesReport>> salesTimeseries(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "DAY") ReportGrouping grouping) {
        return ResponseEntity.ok(handler.salesSeries(restaurantId, null, dateFrom, dateTo, grouping));
    }

    @GetMapping("/payment-methods")
    @RequiresPermission(module = PermissionModule.FINANCE, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<PaymentMethodReportRow>>> paymentMethods(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.paymentMethods(
                restaurantId, null, dateFrom, dateTo, page, size, sort, search));
    }

    @GetMapping("/business-days")
    @RequiresPermission(
            module = PermissionModule.CASH,
            action = PermissionAction.VIEW,
            additional = @PermissionRequirement(module = PermissionModule.FINANCE, action = PermissionAction.VIEW))
    public ResponseEntity<ApiGenericResponse<PageResponse<BusinessDayReportRow>>> businessDays(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.businessDays(
                restaurantId, null, dateFrom, dateTo, page, size, sort, search));
    }

    @GetMapping("/business-days/{businessDayId}")
    @RequiresPermission(
            module = PermissionModule.CASH,
            action = PermissionAction.VIEW,
            additional = @PermissionRequirement(module = PermissionModule.FINANCE, action = PermissionAction.VIEW))
    public ResponseEntity<ApiGenericResponse<BusinessDayDetailReport>> businessDayDetail(
            @PathVariable UUID restaurantId,
            @PathVariable UUID businessDayId) {
        return ResponseEntity.ok(handler.businessDayDetail(restaurantId, businessDayId));
    }

    @GetMapping("/products")
    @RequiresPermission(module = PermissionModule.FINANCE, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<CatalogReportRow>>> products(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.products(
                restaurantId, null, dateFrom, dateTo, page, size, sort, search));
    }

    @GetMapping("/templates")
    @RequiresPermission(module = PermissionModule.FINANCE, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<CatalogReportRow>>> templates(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.templates(
                restaurantId, null, dateFrom, dateTo, page, size, sort, search));
    }

    @GetMapping("/inventory/consumption")
    @RequiresPermission(module = PermissionModule.INVENTORY, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<InventoryConsumptionRow>>> inventoryConsumption(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.inventoryConsumption(
                restaurantId, null, dateFrom, dateTo, page, size, sort, search));
    }

    @GetMapping("/inventory/valuation")
    @RequiresPermission(module = PermissionModule.INVENTORY, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<InventoryValuationReport>> inventoryValuation(
            @PathVariable UUID restaurantId) {
        return ResponseEntity.ok(handler.inventoryValuation(restaurantId, null));
    }

    @GetMapping("/inventory/low-stock")
    @RequiresPermission(module = PermissionModule.INVENTORY, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<LowStockReportRow>>> lowStock(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.lowStock(restaurantId, null, page, size, sort, search));
    }
}
