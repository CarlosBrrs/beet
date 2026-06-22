package com.beet.backend.modules.cash.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class CashSessionDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID businessDayId;
    private UUID cashRegisterId;
    private CashSessionStatus status;
    private OffsetDateTime openedAt;
    private UUID openedBy;
    private UUID openedDeviceId;
    private BigDecimal openingAmount;
    private OffsetDateTime closedAt;
    private UUID closedBy;
    private UUID closedDeviceId;
    private BigDecimal closingAmount;
    private String notes;
}
