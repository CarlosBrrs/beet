package com.beet.backend.modules.cash.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
public class CashSessionReconciliationDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID businessDayId;
    private UUID cashSessionId;
    private BigDecimal openingAmount;
    private BigDecimal paymentsTotal;
    private BigDecimal tipsTotal;
    private BigDecimal refundsTotal;
    private BigDecimal cashInTotal;
    private BigDecimal cashOutTotal;
    private BigDecimal expectedCash;
    private BigDecimal countedCash;
    private BigDecimal differenceAmount;
    private String differenceReason;
    private String notes;
    private OffsetDateTime closedAt;
    private UUID closedBy;
    private UUID closedDeviceId;
    private boolean blind;
    private boolean closed;
    private List<PaymentTotalSnapshot> paymentTotals;
}
