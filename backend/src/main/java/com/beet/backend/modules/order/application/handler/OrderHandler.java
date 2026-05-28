package com.beet.backend.modules.order.application.handler;

import com.beet.backend.modules.order.application.dto.AddOrderItemRequest;
import com.beet.backend.modules.order.application.dto.OrderCreateRequest;
import com.beet.backend.modules.order.application.dto.OrderDetailResponse;
import com.beet.backend.modules.order.application.dto.OrderItemQuantityRequest;
import com.beet.backend.modules.order.application.dto.OrderResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.UUID;

public interface OrderHandler {

    ApiGenericResponse<OrderDetailResponse> create(UUID restaurantId, OrderCreateRequest request);

    ApiGenericResponse<OrderDetailResponse> addItem(UUID restaurantId, UUID orderId, AddOrderItemRequest request);

    ApiGenericResponse<OrderDetailResponse> updateItemQuantity(UUID restaurantId, UUID orderId,
            UUID orderItemId, OrderItemQuantityRequest request);

    ApiGenericResponse<OrderDetailResponse> removeItem(UUID restaurantId, UUID orderId, UUID orderItemId);

    ApiGenericResponse<OrderDetailResponse> getById(UUID restaurantId, UUID orderId);

    ApiGenericResponse<PageResponse<OrderResponse>> list(UUID restaurantId, int page, int size, String search);
}
