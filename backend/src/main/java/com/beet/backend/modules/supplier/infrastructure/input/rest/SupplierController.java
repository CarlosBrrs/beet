package com.beet.backend.modules.supplier.infrastructure.input.rest;

import com.beet.backend.modules.supplier.application.dto.SupplierActivationRequest;
import com.beet.backend.modules.supplier.application.dto.SupplierRequest;
import com.beet.backend.modules.supplier.application.dto.SupplierResponse;
import com.beet.backend.modules.supplier.application.handler.SupplierHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierHandler handler;

    @GetMapping
    public ResponseEntity<ApiGenericResponse<?>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "false") boolean sortDesc,
            @RequestParam(defaultValue = "false") boolean unpagedActive) {
        UUID ownerId = SecurityUtils.getEffectiveOwnerId();
        if (unpagedActive) {
            ApiGenericResponse<List<SupplierResponse>> response = handler.findAllActive(ownerId);
            return ResponseEntity.ok(response);
        }
        ApiGenericResponse<PageResponse<SupplierResponse>> response = handler.list(
                ownerId, page, size, search, active, sortBy, sortDesc);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{supplierId}")
    public ResponseEntity<ApiGenericResponse<SupplierResponse>> findById(@PathVariable UUID supplierId) {
        UUID ownerId = SecurityUtils.getEffectiveOwnerId();
        return ResponseEntity.ok(handler.findById(supplierId, ownerId));
    }

    @PostMapping
    public ResponseEntity<ApiGenericResponse<SupplierResponse>> create(@Valid @RequestBody SupplierRequest request) {
        UUID ownerId = SecurityUtils.getEffectiveOwnerId();
        return ResponseEntity.status(HttpStatus.CREATED).body(handler.create(request, ownerId));
    }

    @PutMapping("/{supplierId}")
    public ResponseEntity<ApiGenericResponse<SupplierResponse>> update(
            @PathVariable UUID supplierId,
            @Valid @RequestBody SupplierRequest request) {
        UUID ownerId = SecurityUtils.getEffectiveOwnerId();
        return ResponseEntity.ok(handler.update(supplierId, request, ownerId));
    }

    @PatchMapping("/{supplierId}/activation")
    public ResponseEntity<ApiGenericResponse<SupplierResponse>> setActive(
            @PathVariable UUID supplierId,
            @Valid @RequestBody SupplierActivationRequest request) {
        UUID ownerId = SecurityUtils.getEffectiveOwnerId();
        return ResponseEntity.ok(handler.setActive(supplierId, request, ownerId));
    }

    @DeleteMapping("/{supplierId}")
    public ResponseEntity<ApiGenericResponse<Void>> delete(@PathVariable UUID supplierId) {
        UUID ownerId = SecurityUtils.getEffectiveOwnerId();
        UUID actorId = SecurityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(handler.delete(supplierId, ownerId, actorId));
    }
}
