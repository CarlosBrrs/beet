package com.beet.backend.modules.cash.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
public class CashMovementDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID businessDayId;
    private UUID cashSessionId;
    private CashMovementDirection direction;
    private CashMovementReason reason;
    private BigDecimal amount;
    private CashMovementStatus status;
    private String notes;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private UUID createdDeviceId;
    private OffsetDateTime voidedAt;
    private UUID voidedBy;
    private UUID voidedDeviceId;
    private String voidReason;
}
