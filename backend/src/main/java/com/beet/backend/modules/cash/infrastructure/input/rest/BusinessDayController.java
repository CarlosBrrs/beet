package com.beet.backend.modules.cash.infrastructure.input.rest;

import com.beet.backend.modules.cash.application.dto.BusinessDayClosureResponse;
import com.beet.backend.modules.cash.application.dto.BusinessDayResponse;
import com.beet.backend.modules.cash.application.dto.CloseBusinessDayRequest;
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
@RequestMapping("/restaurants/{restaurantId}/business-days")
@RequiredArgsConstructor
public class BusinessDayController {
    private final CashOperationsHandler handler;

    @GetMapping("/current")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<BusinessDayResponse>> current(@PathVariable UUID restaurantId) {
        return ResponseEntity.ok(handler.currentBusinessDay(restaurantId));
    }

    @GetMapping
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<BusinessDayResponse>>> list(@PathVariable UUID restaurantId) {
        return ResponseEntity.ok(handler.listBusinessDays(restaurantId));
    }

    @GetMapping("/{businessDayId}")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<BusinessDayResponse>> detail(
            @PathVariable UUID restaurantId, @PathVariable UUID businessDayId) {
        return ResponseEntity.ok(handler.getBusinessDay(restaurantId, businessDayId));
    }

    @PostMapping("/open")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.MANAGE)
    public ResponseEntity<ApiGenericResponse<BusinessDayResponse>> open(@PathVariable UUID restaurantId) {
        return ResponseEntity.ok(handler.openBusinessDay(restaurantId));
    }

    @PostMapping("/{businessDayId}/close")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.MANAGE)
    public ResponseEntity<ApiGenericResponse<BusinessDayClosureResponse>> close(
            @PathVariable UUID restaurantId,
            @PathVariable UUID businessDayId,
            @RequestBody(required = false) CloseBusinessDayRequest request) {
        return ResponseEntity.ok(handler.closeBusinessDay(
                restaurantId, businessDayId, request == null ? null : request.notes()));
    }

    @PostMapping("/{businessDayId}/reopen")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.MANAGE)
    public ResponseEntity<ApiGenericResponse<BusinessDayResponse>> reopen(
            @PathVariable UUID restaurantId,
            @PathVariable UUID businessDayId,
            @Valid @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(handler.reopenBusinessDay(restaurantId, businessDayId, request.reason()));
    }
}
