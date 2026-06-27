package com.beet.backend.modules.order.domain.strategy;

import com.beet.backend.modules.order.domain.model.BillPaymentStatus;
import com.beet.backend.modules.order.domain.model.BillSplitMode;
import com.beet.backend.modules.order.domain.model.OrderBillAllocationDomain;
import com.beet.backend.modules.order.domain.model.OrderBillDomain;
import com.beet.backend.modules.order.domain.model.OrderDomain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

abstract class AbstractBillSplitStrategy implements BillSplitStrategy {
    static final int MONEY_SCALE = 4;

    OrderBillDomain bill(OrderDomain order, String label, BillSplitMode mode, BigDecimal amount,
            List<OrderBillAllocationDomain> allocations, UUID userId) {
        return OrderBillDomain.builder()
                .restaurantId(order.getRestaurantId())
                .orderId(order.getId())
                .label(label == null || label.isBlank() ? "Cuenta" : label.trim())
                .splitMode(mode)
                .subtotalGrossSnapshot(scale(amount))
                .tipTotalSnapshot(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .totalPaidSnapshot(BigDecimal.ZERO.setScale(MONEY_SCALE))
                .paymentStatus(BillPaymentStatus.UNPAID)
                .createdBy(userId)
                .updatedBy(userId)
                .allocations(allocations)
                .build();
    }

    BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
