package com.beet.backend.modules.order.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AddOrderItemRequest(
        String lineType,
        UUID itemId,
        UUID templateId,
        UUID submenuNodeId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String notes,
        List<OrderCreateRequest.TemplateSlotSelectionRequest> slots) {
}
