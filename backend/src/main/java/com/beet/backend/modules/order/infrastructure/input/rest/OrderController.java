package com.beet.backend.modules.order.infrastructure.input.rest;

import com.beet.backend.modules.order.application.dto.AddOrderItemRequest;
import com.beet.backend.modules.order.application.dto.CancelOrderRequest;
import com.beet.backend.modules.order.application.dto.KitchenTicketStatusRequest;
import com.beet.backend.modules.order.application.dto.OrderBillResponse;
import com.beet.backend.modules.order.application.dto.OrderCreateRequest;
import com.beet.backend.modules.order.application.dto.OrderDetailResponse;
import com.beet.backend.modules.order.application.dto.OrderItemQuantityRequest;
import com.beet.backend.modules.order.application.dto.OrderResponse;
import com.beet.backend.modules.order.application.dto.PaymentMethodRequest;
import com.beet.backend.modules.order.application.dto.PaymentMethodResponse;
import com.beet.backend.modules.order.application.dto.PaymentRequest;
import com.beet.backend.modules.order.application.dto.PaymentResponse;
import com.beet.backend.modules.order.application.dto.PosCatalogResponse;
import com.beet.backend.modules.order.application.dto.SplitBillsRequest;
import com.beet.backend.modules.order.application.handler.OrderHandler;
import com.beet.backend.modules.order.domain.model.BillPaymentStatus;
import com.beet.backend.modules.order.domain.model.DeliveryStatus;
import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.KitchenTicketStatus;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderHandler handler;

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.VIEW)
    @GetMapping("/restaurants/{restaurantId}/pos/catalog")
    public ResponseEntity<ApiGenericResponse<PageResponse<PosCatalogResponse>>> posCatalog(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID menuId,
            @RequestParam(required = false) UUID submenuId,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(handler.posCatalog(
                restaurantId, page, size, search, menuId, submenuId, availability, referenceType, sort));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.CREATE)
    @PostMapping("/restaurants/{restaurantId}/orders/draft")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse>> createDraft(
            @PathVariable UUID restaurantId,
            @RequestBody OrderCreateRequest request) {
        return ResponseEntity.ok(handler.createDraft(restaurantId, request));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.CREATE)
    @PostMapping("/restaurants/{restaurantId}/orders")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse>> create(
            @PathVariable UUID restaurantId,
            @RequestBody OrderCreateRequest request) {
        return ResponseEntity.ok(handler.create(restaurantId, request));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    @PostMapping("/restaurants/{restaurantId}/orders/{orderId}/confirm")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse>> confirm(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(handler.confirm(restaurantId, orderId));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    @PostMapping("/restaurants/{restaurantId}/orders/{orderId}/complete")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse>> complete(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(handler.complete(restaurantId, orderId));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.EDIT)
    @PostMapping("/restaurants/{restaurantId}/orders/{orderId}/cancel")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse>> cancel(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(handler.cancel(restaurantId, orderId, request));
    }

    @RequiresPermission(module = PermissionModule.ORDERS, action = PermissionAction.VIEW)
    @GetMapping("/restaurants/{restaurantId}/orders")
    public ResponseEntity<ApiGenericResponse<PageResponse<OrderResponse>>> list(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) KitchenStatus kitchenStatus,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) UUID tableId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(required = false) UUID cashSessionId,
            @RequestParam(required = false) UUID cashRegisterId,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) UUID paymentMethodId,
            @RequestParam(required = false) BigDecimal minTotal,
            @RequestParam(required = false) BigDecimal maxTotal,
            @RequestParam(required = false) DeliveryStatus deliveryStatus,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(handler.list(restaurantId, page, size, sort, orderStatus, paymentStatus,
                kitchenStatus, serviceType, tableId, dateFrom, dateTo, cashSessionId, cashRegisterId, createdBy,
                customer, paymentMethodId, minTotal, maxTotal, deliveryStatus, search));
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

    @RequiresPermission(module = PermissionModule.KDS, action = PermissionAction.VIEW)
    @GetMapping("/restaurants/{restaurantId}/kitchen-tickets")
    public ResponseEntity<ApiGenericResponse<PageResponse<OrderDetailResponse.KitchenTicketResponse>>> listKitchenTickets(
            @PathVariable UUID restaurantId,
            @RequestParam(required = false) KitchenTicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(handler.listKitchenTickets(restaurantId, status, page, size));
    }

    @RequiresPermission(module = PermissionModule.KDS, action = PermissionAction.UPDATE_STATUS)
    @PatchMapping("/restaurants/{restaurantId}/kitchen-tickets/{ticketId}/status")
    public ResponseEntity<ApiGenericResponse<OrderDetailResponse.KitchenTicketResponse>> updateKitchenTicket(
            @PathVariable UUID restaurantId,
            @PathVariable UUID ticketId,
            @RequestBody KitchenTicketStatusRequest request) {
        return ResponseEntity.ok(handler.updateKitchenTicket(restaurantId, ticketId, request));
    }

    @RequiresPermission(module = PermissionModule.PAYMENTS, action = PermissionAction.VIEW)
    @GetMapping("/restaurants/{restaurantId}/payment-methods")
    public ResponseEntity<ApiGenericResponse<List<PaymentMethodResponse>>> listPaymentMethods(
            @PathVariable UUID restaurantId) {
        return ResponseEntity.ok(handler.listPaymentMethods(restaurantId));
    }

    @RequiresPermission(module = PermissionModule.PAYMENTS, action = PermissionAction.CREATE)
    @PostMapping("/restaurants/{restaurantId}/payment-methods")
    public ResponseEntity<ApiGenericResponse<PaymentMethodResponse>> createPaymentMethod(
            @PathVariable UUID restaurantId,
            @RequestBody PaymentMethodRequest request) {
        return ResponseEntity.ok(handler.createPaymentMethod(restaurantId, request));
    }

    @RequiresPermission(module = PermissionModule.PAYMENTS, action = PermissionAction.EDIT)
    @PatchMapping("/restaurants/{restaurantId}/payment-methods/{methodId}")
    public ResponseEntity<ApiGenericResponse<PaymentMethodResponse>> updatePaymentMethod(
            @PathVariable UUID restaurantId,
            @PathVariable UUID methodId,
            @RequestBody PaymentMethodRequest request) {
        return ResponseEntity.ok(handler.updatePaymentMethod(restaurantId, methodId, request));
    }

    @RequiresPermission(module = PermissionModule.PAYMENTS, action = PermissionAction.CREATE)
    @PostMapping("/restaurants/{restaurantId}/orders/{orderId}/payments")
    public ResponseEntity<ApiGenericResponse<PaymentResponse>> registerPayment(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(handler.registerPayment(restaurantId, orderId, request));
    }

    @RequiresPermission(module = PermissionModule.PAYMENTS, action = PermissionAction.CREATE)
    @PostMapping("/restaurants/{restaurantId}/orders/{orderId}/bills/split")
    public ResponseEntity<ApiGenericResponse<List<OrderBillResponse>>> splitBills(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @RequestBody SplitBillsRequest request) {
        return ResponseEntity.ok(handler.splitBills(restaurantId, orderId, request));
    }

    @RequiresPermission(module = PermissionModule.PAYMENTS, action = PermissionAction.VIEW)
    @GetMapping("/restaurants/{restaurantId}/orders/{orderId}/bills")
    public ResponseEntity<ApiGenericResponse<PageResponse<OrderBillResponse>>> listBills(
            @PathVariable UUID restaurantId,
            @PathVariable UUID orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BillPaymentStatus paymentStatus,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(handler.listBills(restaurantId, orderId, page, size, paymentStatus, search, sort));
    }
}
