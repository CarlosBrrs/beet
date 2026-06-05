package com.beet.backend.modules.order.domain.model;

import java.util.UUID;

public record BillSearchCriteria(
        UUID restaurantId,
        UUID orderId,
        int page,
        int size,
        String sort,
        BillPaymentStatus paymentStatus,
        String search) {
}
