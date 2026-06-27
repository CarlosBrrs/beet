package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PaymentRefundDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID orderId;
    private UUID paymentId;
    private UUID cashSessionId;
    private UUID deviceId;
    private BigDecimal amount;
    private PaymentRefundStatus status;
    private String reason;
    private String externalReference;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime voidedAt;
    private UUID voidedBy;
}
