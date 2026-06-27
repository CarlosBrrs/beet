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
public class PaymentDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID orderId;
    private UUID orderBillId;
    private UUID paymentMethodId;
    private UUID cashSessionId;
    private UUID deviceId;
    private BigDecimal amount;
    private BigDecimal tipAmount;
    private PaymentRecordStatus status;
    private String externalReference;
    private String notes;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime voidedAt;
    private UUID voidedBy;
}
