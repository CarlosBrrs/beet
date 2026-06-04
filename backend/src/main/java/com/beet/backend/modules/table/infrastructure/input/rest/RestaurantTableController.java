package com.beet.backend.modules.table.infrastructure.input.rest;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.modules.table.application.dto.CreateRestaurantTableRequest;
import com.beet.backend.modules.table.application.dto.RestaurantTableResponse;
import com.beet.backend.modules.table.application.dto.UpdateRestaurantTableRequest;
import com.beet.backend.modules.table.application.handler.RestaurantTableHandler;
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
@RequestMapping("/restaurants/{restaurantId}/tables")
@RequiredArgsConstructor
public class RestaurantTableController {
    private final RestaurantTableHandler handler;

    @PostMapping
    @RequiresPermission(module = PermissionModule.TABLES, action = PermissionAction.CREATE)
    public ResponseEntity<ApiGenericResponse<RestaurantTableResponse>> create(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody CreateRestaurantTableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(handler.create(restaurantId, request));
    }

    @GetMapping
    @RequiresPermission(module = PermissionModule.TABLES, action = PermissionAction.VIEW)
    public ResponseEntity<ApiGenericResponse<List<RestaurantTableResponse>>> list(
            @PathVariable UUID restaurantId) {
        return ResponseEntity.ok(handler.list(restaurantId));
    }

    @PatchMapping("/{tableId}")
    @RequiresPermission(module = PermissionModule.TABLES, action = PermissionAction.EDIT)
    public ResponseEntity<ApiGenericResponse<RestaurantTableResponse>> update(
            @PathVariable UUID restaurantId,
            @PathVariable UUID tableId,
            @Valid @RequestBody UpdateRestaurantTableRequest request) {
        return ResponseEntity.ok(handler.update(restaurantId, tableId, request));
    }

    @DeleteMapping("/{tableId}")
    @RequiresPermission(module = PermissionModule.TABLES, action = PermissionAction.DELETE)
    public ResponseEntity<ApiGenericResponse<RestaurantTableResponse>> deactivate(
            @PathVariable UUID restaurantId,
            @PathVariable UUID tableId) {
        return ResponseEntity.ok(handler.deactivate(restaurantId, tableId));
    }
}
