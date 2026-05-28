package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.ServiceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        ServiceType serviceType,
        UUID tableId,
        String customerName,
        String notes,
        List<OrderItemRequest> items) {

    public record OrderItemRequest(
            UUID itemId,
            UUID submenuNodeId,
            BigDecimal quantity,
            BigDecimal unitPrice) {
    }
}
