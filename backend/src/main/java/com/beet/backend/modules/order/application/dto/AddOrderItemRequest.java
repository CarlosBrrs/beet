package com.beet.backend.modules.order.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AddOrderItemRequest(
        UUID itemId,
        UUID submenuNodeId,
        BigDecimal quantity,
        BigDecimal unitPrice) {
}
