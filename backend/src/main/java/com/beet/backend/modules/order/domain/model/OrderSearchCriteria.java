package com.beet.backend.modules.order.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderSearchCriteria(
        UUID restaurantId,
        int page,
        int size,
        String sort,
        OrderStatus orderStatus,
        PaymentStatus paymentStatus,
        KitchenStatus kitchenStatus,
        ServiceType serviceType,
        UUID tableId,
        OffsetDateTime dateFrom,
        OffsetDateTime dateTo,
        UUID cashSessionId,
        UUID cashRegisterId,
        UUID createdBy,
        String customer,
        UUID paymentMethodId,
        BigDecimal minTotal,
        BigDecimal maxTotal,
        DeliveryStatus deliveryStatus,
        PaymentPendingState paymentPendingState,
        String search) {
}
