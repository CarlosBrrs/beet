package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.BillPaymentStatus;
import com.beet.backend.modules.order.domain.model.BillSplitMode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderBillResponse(
        UUID id,
        UUID orderId,
        String label,
        BillSplitMode splitMode,
        BigDecimal subtotalGrossSnapshot,
        BigDecimal tipTotalSnapshot,
        BigDecimal totalPaidSnapshot,
        BillPaymentStatus paymentStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<OrderBillAllocationResponse> allocations) {

    public record OrderBillAllocationResponse(
            UUID id,
            UUID orderItemId,
            BigDecimal quantity,
            BigDecimal amount) {
    }
}
