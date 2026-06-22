package com.beet.backend.modules.order.domain.model;

public enum PaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    REFUND_PENDING,
    REFUNDED
}
