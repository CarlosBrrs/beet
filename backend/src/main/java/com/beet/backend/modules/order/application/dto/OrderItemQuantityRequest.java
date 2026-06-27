package com.beet.backend.modules.order.application.dto;

import java.math.BigDecimal;

public record OrderItemQuantityRequest(BigDecimal quantity) {
}
