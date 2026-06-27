package com.beet.backend.modules.order.application.dto;

import com.beet.backend.modules.order.domain.model.BillSplitMode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SplitBillsRequest(
        BillSplitMode mode,
        List<SplitBillRequest> bills) {

    public record SplitBillRequest(
            String label,
            BigDecimal amount,
            BigDecimal percentage,
            List<SplitBillItemRequest> items) {
    }

    public record SplitBillItemRequest(UUID orderItemId, BigDecimal quantity) {
    }
}
