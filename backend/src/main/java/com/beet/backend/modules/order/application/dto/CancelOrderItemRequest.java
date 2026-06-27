package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.OrderItemInventoryDisposition;

import java.math.BigDecimal;

public record CancelOrderItemRequest(
        BigDecimal quantity,
        String reason,
        OrderItemInventoryDisposition inventoryDisposition) {
}
