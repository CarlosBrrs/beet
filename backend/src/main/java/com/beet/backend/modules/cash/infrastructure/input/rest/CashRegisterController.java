package com.beet.backend.modules.cash.infrastructure.input.rest;

import com.beet.backend.modules.cash.application.dto.CashRegisterResponse;
import com.beet.backend.modules.cash.application.dto.CreateCashRegisterRequest;
import com.beet.backend.modules.cash.application.dto.UpdateCashRegisterRequest;
import com.beet.backend.modules.cash.application.handler.CashHandler;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants/{restaurantId}/cash-registers")
@RequiredArgsConstructor
public class CashRegisterController {

    private final CashHandler handler;

    @PostMapping
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.CREATE)
    public ResponseEntity<ApiGenericResponse<CashRegisterResponse>> create(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody CreateCashRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(handler.createRegister(restaurantId, request));
    }

    @GetMapping
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<CashRegisterResponse>>> list(
            @PathVariable UUID restaurantId) {
        return ResponseEntity.ok(handler.listRegisters(restaurantId));
    }

    @PatchMapping("/{registerId}")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<CashRegisterResponse>> update(
            @PathVariable UUID restaurantId,
            @PathVariable UUID registerId,
            @Valid @RequestBody UpdateCashRegisterRequest request) {
        return ResponseEntity.ok(handler.updateRegister(restaurantId, registerId, request));
    }

    @DeleteMapping("/{registerId}")
    @RequiresPermission(module = PermissionModule.CASH, action = PermissionAction.DELETE)
    public ResponseEntity<ApiGenericResponse<CashRegisterResponse>> deactivate(
            @PathVariable UUID restaurantId,
            @PathVariable UUID registerId) {
        return ResponseEntity.ok(handler.deactivateRegister(restaurantId, registerId));
    }
}
