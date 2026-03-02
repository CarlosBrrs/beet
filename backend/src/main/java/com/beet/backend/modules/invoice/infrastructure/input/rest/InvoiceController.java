package com.beet.backend.modules.invoice.infrastructure.input.rest;

import com.beet.backend.modules.invoice.application.dto.*;
import com.beet.backend.modules.invoice.application.handler.InvoiceHandler;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for invoice (purchase) operations.
 * All endpoints are scoped to a restaurant and require INVOICES permissions.
 */
@RestController
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceHandler handler;

    // ── Register Invoice ──

    @RequiresPermission(module = PermissionModule.INVOICES, action = PermissionAction.CREATE)
    @PostMapping("/restaurants/{restaurantId}/invoices")
    public ResponseEntity<ApiGenericResponse<InvoiceResponse>> register(
            @PathVariable UUID restaurantId,
            @RequestBody RegisterInvoiceRequest request) {
        return ResponseEntity.ok(handler.register(restaurantId, request));
    }

    // ── List Invoices (paginated) ──

    @RequiresPermission(module = PermissionModule.INVOICES, action = PermissionAction.VIEW)
    @GetMapping("/restaurants/{restaurantId}/invoices")
    public ResponseEntity<ApiGenericResponse<PageResponse<InvoiceResponse>>> list(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.list(restaurantId, page, size, search));
    }

    // ── Invoice Detail ──

    @RequiresPermission(module = PermissionModule.INVOICES, action = PermissionAction.VIEW)
    @GetMapping("/restaurants/{restaurantId}/invoices/{invoiceId}")
    public ResponseEntity<ApiGenericResponse<InvoiceDetailResponse>> detail(
            @PathVariable UUID restaurantId,
            @PathVariable UUID invoiceId) {
        return ResponseEntity.ok(handler.getDetail(restaurantId, invoiceId));
    }

    // ── Supplier Items for Invoice Form ──

    @RequiresPermission(module = PermissionModule.INVOICES, action = PermissionAction.CREATE)
    @GetMapping("/restaurants/{restaurantId}/suppliers/{supplierId}/items")
    public ResponseEntity<ApiGenericResponse<List<SupplierItemForInvoiceResponse>>> supplierItems(
            @PathVariable UUID restaurantId,
            @PathVariable UUID supplierId) {
        return ResponseEntity.ok(handler.getSupplierItems(supplierId));
    }
}
