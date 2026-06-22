package com.beet.backend.modules.cash.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class BusinessDayClosureDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID businessDayId;
    private int closureSequence;
    private BigDecimal paymentsTotal;
    private BigDecimal tipsTotal;
    private BigDecimal refundsTotal;
    private BigDecimal cashInTotal;
    private BigDecimal cashOutTotal;
    private BigDecimal expectedCashTotal;
    private BigDecimal countedCashTotal;
    private BigDecimal differenceTotal;
    private String notes;
    private OffsetDateTime closedAt;
    private UUID closedBy;
    private List<PaymentTotalSnapshot> paymentTotals;
}
