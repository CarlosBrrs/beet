package com.beet.backend.modules.order.application.handler;

import com.beet.backend.modules.order.application.dto.AddOrderItemRequest;
import com.beet.backend.modules.order.application.dto.OrderCreateRequest;
import com.beet.backend.modules.order.application.dto.OrderDetailResponse;
import com.beet.backend.modules.order.application.dto.OrderItemQuantityRequest;
import com.beet.backend.modules.order.application.dto.OrderResponse;
import com.beet.backend.modules.order.domain.api.OrderServicePort;
import com.beet.backend.modules.order.domain.exception.OrderNotFoundException;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTaxDomain;
import com.beet.backend.modules.order.domain.model.OrderTaxDomain;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderHandlerImpl implements OrderHandler {

    private final OrderServicePort orderService;

    @Override
    public ApiGenericResponse<OrderDetailResponse> create(UUID restaurantId, OrderCreateRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        OrderDomain order = OrderDomain.builder()
                .restaurantId(restaurantId)
                .serviceType(request.serviceType())
                .tableId(request.tableId())
                .customerName(request.customerName())
                .notes(request.notes())
                .items(toOrderItemDomains(request.items()))
                .build();

        OrderDomain created = orderService.createOrder(order, userId);
        return ApiGenericResponse.success(toDetailResponse(created));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> addItem(UUID restaurantId, UUID orderId,
            AddOrderItemRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        OrderItemDomain item = OrderItemDomain.builder()
                .itemId(request.itemId())
                .submenuNodeId(request.submenuNodeId())
                .quantity(request.quantity())
                .unitPriceSnapshot(request.unitPrice())
                .build();

        OrderDomain updated = orderService.addItem(orderId, item, userId);
        return ApiGenericResponse.success(toDetailResponse(updated));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> updateItemQuantity(UUID restaurantId, UUID orderId,
            UUID orderItemId, OrderItemQuantityRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        OrderDomain updated = orderService.updateItemQuantity(orderId, orderItemId, request.quantity(), userId);
        return ApiGenericResponse.success(toDetailResponse(updated));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> removeItem(UUID restaurantId, UUID orderId, UUID orderItemId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        OrderDomain updated = orderService.removeItem(orderId, orderItemId, userId);
        return ApiGenericResponse.success(toDetailResponse(updated));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> getById(UUID restaurantId, UUID orderId) {
        OrderDomain found = orderService.findById(orderId)
                .orElseThrow(() -> OrderNotFoundException.forId(orderId));
        return ApiGenericResponse.success(toDetailResponse(found));
    }

    @Override
    public ApiGenericResponse<PageResponse<OrderResponse>> list(UUID restaurantId, int page, int size, String search) {
        PageResponse<OrderDomain> result = orderService.findAllPaged(restaurantId, page, size, search);
        List<OrderResponse> content = result.content().stream()
                .map(this::toResponse)
                .toList();
        return ApiGenericResponse.success(PageResponse.of(content, result.totalElements(), page, size));
    }

    private List<OrderItemDomain> toOrderItemDomains(List<OrderCreateRequest.OrderItemRequest> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(item -> OrderItemDomain.builder()
                        .itemId(item.itemId())
                        .submenuNodeId(item.submenuNodeId())
                        .quantity(item.quantity())
                        .unitPriceSnapshot(item.unitPrice())
                        .build())
                .toList();
    }

    private OrderResponse toResponse(OrderDomain order) {
        return new OrderResponse(
                order.getId(),
                order.getRestaurantId(),
                order.getOrderStatus(),
                order.getKitchenStatus(),
                order.getPaymentStatus(),
                order.getServiceType(),
                order.getTableId(),
                order.getCustomerName(),
                order.getSubtotalGrossSnapshot(),
                order.getTaxAmountSnapshot(),
                order.getTotalGrossSnapshot(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private OrderDetailResponse toDetailResponse(OrderDomain order) {
        List<OrderDetailResponse.OrderItemResponse> items = order.getItems() != null
                ? order.getItems().stream().map(this::toItemResponse).toList()
                : List.of();
        List<OrderDetailResponse.OrderTaxResponse> taxes = order.getTaxes() != null
                ? order.getTaxes().stream().map(this::toTaxResponse).toList()
                : List.of();

        return new OrderDetailResponse(
                order.getId(),
                order.getRestaurantId(),
                order.getOrderStatus(),
                order.getKitchenStatus(),
                order.getPaymentStatus(),
                order.getServiceType(),
                order.getTableId(),
                order.getCustomerName(),
                order.isPrepaymentRequiredSnapshot(),
                order.getTaxRateSnapshot(),
                order.getSubtotalGrossSnapshot(),
                order.getTaxAmountSnapshot(),
                order.getTotalGrossSnapshot(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items,
                taxes);
    }

    private OrderDetailResponse.OrderItemResponse toItemResponse(OrderItemDomain item) {
        List<OrderDetailResponse.OrderItemTaxResponse> taxes = item.getTaxes() != null
                ? item.getTaxes().stream().map(this::toItemTaxResponse).toList()
                : List.of();
        return new OrderDetailResponse.OrderItemResponse(
                item.getId(),
                item.getItemId(),
                item.getSubmenuNodeId(),
                item.getItemNameSnapshot(),
                item.getUnitPriceSnapshot(),
                item.getTheoreticalCostSnapshot(),
                item.getQuantity(),
                item.getSubtotalGrossSnapshot(),
                taxes);
    }

    private OrderDetailResponse.OrderTaxResponse toTaxResponse(OrderTaxDomain tax) {
        return new OrderDetailResponse.OrderTaxResponse(
                tax.getId(),
                tax.getTaxId(),
                tax.getTaxNameSnapshot(),
                tax.getTaxRateSnapshot(),
                tax.getTaxBaseSnapshot(),
                tax.getTaxAmountSnapshot());
    }

    private OrderDetailResponse.OrderItemTaxResponse toItemTaxResponse(OrderItemTaxDomain tax) {
        return new OrderDetailResponse.OrderItemTaxResponse(
                tax.getId(),
                tax.getOrderItemId(),
                tax.getTaxId(),
                tax.getTaxNameSnapshot(),
                tax.getTaxRateSnapshot(),
                tax.getTaxBaseSnapshot(),
                tax.getTaxAmountSnapshot());
    }
}
