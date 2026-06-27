package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.PaymentMethodType;

public record PaymentMethodRequest(
        String code,
        String name,
        PaymentMethodType type,
        Boolean isActive,
        Boolean requiresReference,
        Integer sortOrder) {
}
