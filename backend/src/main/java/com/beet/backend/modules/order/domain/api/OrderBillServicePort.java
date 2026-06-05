package com.beet.backend.modules.order.domain.api;

import com.beet.backend.modules.order.domain.model.BillSearchCriteria;
import com.beet.backend.modules.order.domain.model.BillSplitMode;
import com.beet.backend.modules.order.domain.model.OrderBillDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderBillServicePort {
    List<OrderBillDomain> split(UUID restaurantId, UUID orderId, BillSplitMode mode,
            List<SplitBillCommand> bills, UUID userId);

    PageResponse<OrderBillDomain> list(BillSearchCriteria criteria);

    record SplitBillCommand(
            String label,
            BigDecimal amount,
            BigDecimal percentage,
            List<SplitBillItemCommand> items) {
    }

    record SplitBillItemCommand(UUID orderItemId, BigDecimal quantity) {
    }
}
