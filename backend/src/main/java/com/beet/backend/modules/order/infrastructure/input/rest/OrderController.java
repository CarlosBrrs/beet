package com.beet.backend.modules.order.infrastructure.input.rest;

import com.beet.backend.modules.order.application.dto.AddOrderItemRequest;
import com.beet.backend.modules.order.application.dto.OrderCreateRequest;
import com.beet.backend.modules.order.application.dto.OrderDetailResponse;
import com.beet.backend.modules.order.application.dto.OrderItemQuantityRequest;
import com.beet.backend.modules.order.application.dto.OrderResponse;
import com.beet.backend.modules.order.application.handler.OrderHandler;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderHandler handler;

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.CREATE)
    @PostMapping("/restaurants/{restaurantId}/orders")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse>> create(
            @PathVariable UUID restaurantId,
            @RequestBody OrderCreateRequest request) {
        return ResponseEntity.ok(handler.create(restaurantId, request));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.VIEW)
    @GetMapping("/restaurants/{restaurantId}/orders")
    public ResponseEntity<ApiGenericResponse<PageResponse<OrderResponse>>> list(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.list(restaurantId, page, size, search));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.VIEW)
    @GetMapping("/restaurants/{restaurantId}/orders/{orderId}")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse>> detail(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(handler.getById(restaurantId, orderId));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    @PostMapping("/restaurants/{restaurantId}/orders/{orderId}/items")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse>> addItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @RequestBody AddOrderItemRequest request) {
        return ResponseEntity.ok(handler.addItem(restaurantId, orderId, request));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    @PatchMapping("/restaurants/{restaurantId}/orders/{orderId}/items/{orderItemId}")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse>> updateQuantity(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @PathVariable UUID orderItemId,
            @RequestBody OrderItemQuantityRequest request) {
        return ResponseEntity.ok(handler.updateItemQuantity(restaurantId, orderId, orderItemId, request));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.DELETE)
    @DeleteMapping("/restaurants/{restaurantId}/orders/{orderId}/items/{orderItemId}")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse>> removeItem(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @PathVariable UUID orderItemId) {
        return ResponseEntity.ok(handler.removeItem(restaurantId, orderId, orderItemId));
    }
}
