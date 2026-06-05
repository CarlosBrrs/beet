package com.beet.backend.modules.order.domain.strategy;

import com.beet.backend.modules.order.domain.api.OrderBillServicePort;
import com.beet.backend.modules.order.domain.model.BillSplitMode;
import com.beet.backend.modules.order.domain.model.OrderBillAllocationDomain;
import com.beet.backend.modules.order.domain.model.OrderBillDomain;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Component
public class PercentageSplitStrategy extends AbstractBillSplitStrategy {
    @Override
    public BillSplitMode mode() {
        return BillSplitMode.PERCENTAGE;
    }

    @Override
    public List<OrderBillDomain> split(OrderDomain order, List<OrderBillServicePort.SplitBillCommand> commands,
            UUID userId) {
        BigDecimal total = scale(order.getTotalGrossSnapshot());
        return commands.stream()
                .map(command -> {
                    BigDecimal amount = total.multiply(scale(command.percentage()))
                            .divide(new BigDecimal("100"), MONEY_SCALE, RoundingMode.HALF_UP);
                    var allocation = OrderBillAllocationDomain.builder()
                            .amount(amount)
                            .build();
                    return bill(order, command.label(), mode(), amount, List.of(allocation), userId);
                })
                .toList();
    }
}
