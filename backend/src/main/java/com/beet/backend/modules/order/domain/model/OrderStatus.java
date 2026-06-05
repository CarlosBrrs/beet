package com.beet.backend.modules.order.domain.model;

public enum OrderStatus {
    DRAFT,
    AWAITING_PAYMENT,
    OPEN,
    COMPLETED,
    CANCELED
}
