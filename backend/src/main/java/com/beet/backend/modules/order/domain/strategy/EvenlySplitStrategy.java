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
public class EvenlySplitStrategy extends AbstractBillSplitStrategy {
    @Override
    public BillSplitMode mode() {
        return BillSplitMode.EVENLY;
    }

    @Override
    public List<OrderBillDomain> split(OrderDomain order, List<OrderBillServicePort.SplitBillCommand> commands,
            UUID userId) {
        if (commands.isEmpty()) {
            throw new IllegalArgumentException("At least one bill is required.");
        }
        BigDecimal amount = scale(order.getTotalGrossSnapshot())
                .divide(BigDecimal.valueOf(commands.size()), MONEY_SCALE, RoundingMode.HALF_UP);
        return commands.stream()
                .map(command -> {
                    var allocation = OrderBillAllocationDomain.builder()
                            .amount(amount)
                            .build();
                    return bill(order, command.label(), mode(), amount, List.of(allocation), userId);
                })
                .toList();
    }
}
