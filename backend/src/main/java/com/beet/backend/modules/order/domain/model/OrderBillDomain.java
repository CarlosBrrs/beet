package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderBillDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID orderId;
    private String label;
    private BillSplitMode splitMode;
    private BigDecimal subtotalGrossSnapshot;
    private BigDecimal tipTotalSnapshot;
    private BigDecimal totalPaidSnapshot;
    private BillPaymentStatus paymentStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    @Builder.Default
    private List<OrderBillAllocationDomain> allocations = new ArrayList<>();
}
