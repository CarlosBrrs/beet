package com.beet.backend.modules.order.domain.api;

import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface OrderServicePort {

    OrderDomain createOrder(OrderDomain order, UUID userId);

    OrderDomain addItem(UUID restaurantId, UUID orderId, OrderItemDomain item, UUID userId);

    OrderDomain updateItemQuantity(UUID restaurantId, UUID orderId, UUID orderItemId, BigDecimal quantity, UUID userId);

    OrderDomain removeItem(UUID restaurantId, UUID orderId, UUID orderItemId, UUID userId);

    Optional<OrderDomain> findById(UUID restaurantId, UUID orderId);

    PageResponse<OrderDomain> findAllPaged(UUID restaurantId, int page, int size, String search);
}
