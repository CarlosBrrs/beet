package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.OrderItemInventoryDisposition;

import java.util.List;
import java.util.UUID;

public record CancelOrderRequest(
        String reason,
        List<LineDecision> lineDecisions) {

    public record LineDecision(
            UUID orderItemId,
            OrderItemInventoryDisposition inventoryDisposition) {
    }
}
