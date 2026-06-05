package com.beet.backend.modules.order.domain.strategy;

import com.beet.backend.modules.order.domain.api.OrderBillServicePort;
import com.beet.backend.modules.order.domain.model.BillSplitMode;
import com.beet.backend.modules.order.domain.model.OrderBillAllocationDomain;
import com.beet.backend.modules.order.domain.model.OrderBillDomain;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class ItemQuantitySplitStrategy extends AbstractBillSplitStrategy {
    @Override
    public BillSplitMode mode() {
        return BillSplitMode.ITEM_QUANTITY;
    }

    @Override
    public List<OrderBillDomain> split(OrderDomain order, List<OrderBillServicePort.SplitBillCommand> commands,
            UUID userId) {
        return commands.stream()
                .map(command -> {
                    List<OrderBillAllocationDomain> allocations = command.items().stream()
                            .map(itemCommand -> {
                                OrderItemDomain item = order.getItems().stream()
                                        .filter(candidate -> candidate.getId().equals(itemCommand.orderItemId()))
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                "Order item not found for bill split: " + itemCommand.orderItemId()));
                                BigDecimal unit = scale(item.getSubtotalGrossSnapshot())
                                        .divide(scale(item.getQuantity()), MONEY_SCALE, java.math.RoundingMode.HALF_UP);
                                BigDecimal amount = unit.multiply(scale(itemCommand.quantity()));
                                return OrderBillAllocationDomain.builder()
                                        .orderItemId(item.getId())
                                        .quantity(scale(itemCommand.quantity()))
                                        .amount(scale(amount))
                                        .build();
                            })
                            .toList();
                    BigDecimal amount = allocations.stream()
                            .map(OrderBillAllocationDomain::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return bill(order, command.label(), mode(), amount, allocations, userId);
                })
                .toList();
    }
}
