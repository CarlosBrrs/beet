package com.beet.backend.modules.cash.infrastructure.input.rest;

import com.beet.backend.modules.cash.application.dto.CashSessionResponse;
import com.beet.backend.modules.cash.application.dto.CashSessionListResponse;
import com.beet.backend.modules.cash.application.dto.CloseCashSessionRequest;
import com.beet.backend.modules.cash.application.dto.OpenCashSessionRequest;
import com.beet.backend.modules.cash.application.handler.CashHandler;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants/{restaurantId}/cash-sessions")
@RequiredArgsConstructor
public class CashSessionController {

    private final CashHandler handler;

    @GetMapping
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<PageResponse<CashSessionListResponse>>> list(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) CashSessionStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "UTC") String timeZone,
            @RequestParam(required = false) UUID cashRegisterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(handler.listSessions(
                restaurantId, status, from, to, timeZone, cashRegisterId, page, size));
    }

    @PostMapping("/open")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.OPEN)
    public ResponseEntity<ApiGenericResponse<CashSessionResponse>> open(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody OpenCashSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(handler.openSession(restaurantId, request));
    }

    @PostMapping("/{sessionId}/close")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.CLOSE)
    public ResponseEntity<ApiGenericResponse<CashSessionResponse>> close(
            @PathVariable UUID restaurantId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CloseCashSessionRequest request) {
        return ResponseEntity.ok(handler.closeSession(restaurantId, sessionId, request));
    }

    @PostMapping("/{sessionId}/force-close")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.MANAGE)
    public ResponseEntity<ApiGenericResponse<CashSessionResponse>> forceClose(
            @PathVariable UUID restaurantId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CloseCashSessionRequest request) {
        return ResponseEntity.ok(handler.forceCloseSession(restaurantId, sessionId, request));
    }

    @GetMapping("/active")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<CashSessionResponse>> active(
            @PathVariable UUID restaurantId) {
        return ResponseEntity.ok(handler.getActiveSession(restaurantId));
    }
}
