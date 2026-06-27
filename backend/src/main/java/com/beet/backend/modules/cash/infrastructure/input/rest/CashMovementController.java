package com.beet.backend.modules.cash.infrastructure.input.rest;

import com.beet.backend.modules.cash.application.dto.CashMovementRequest;
import com.beet.backend.modules.cash.application.dto.CashMovementResponse;
import com.beet.backend.modules.cash.application.dto.CashSessionReconciliationResponse;
import com.beet.backend.modules.cash.application.dto.ReasonRequest;
import com.beet.backend.modules.cash.application.handler.CashOperationsHandler;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants/{restaurantId}/cash-sessions/{sessionId}")
@RequiredArgsConstructor
public class CashMovementController {
    private final CashOperationsHandler handler;

    @GetMapping("/movements")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<CashMovementResponse>>> list(
            @PathVariable UUID restaurantId, @PathVariable UUID sessionId) {
        return ResponseEntity.ok(handler.listMovements(restaurantId, sessionId));
    }

    @PostMapping("/movements")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.PROCESS)
    public ResponseEntity<ApiGenericResponse<CashMovementResponse>> create(
            @PathVariable UUID restaurantId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CashMovementRequest request) {
        return ResponseEntity.ok(handler.recordMovement(restaurantId, sessionId, request));
    }

    @PostMapping("/movements/{movementId}/void")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.VOID)
    public ResponseEntity<ApiGenericResponse<CashMovementResponse>> voidMovement(
            @PathVariable UUID restaurantId,
            @PathVariable UUID sessionId,
            @PathVariable UUID movementId,
            @Valid @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(handler.voidMovement(
                restaurantId, sessionId, movementId, request.reason()));
    }

    @GetMapping("/reconciliation")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<CashSessionReconciliationResponse>> reconciliation(
            @PathVariable UUID restaurantId, @PathVariable UUID sessionId) {
        return ResponseEntity.ok(handler.reconciliation(restaurantId, sessionId));
    }
}
