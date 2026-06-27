package com.beet.backend.modules.order.domain.strategy;

import com.beet.backend.modules.order.domain.api.OrderBillServicePort;
import com.beet.backend.modules.order.domain.model.BillSplitMode;
import com.beet.backend.modules.order.domain.model.OrderBillAllocationDomain;
import com.beet.backend.modules.order.domain.model.OrderBillDomain;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AmountSplitStrategy extends AbstractBillSplitStrategy {
    @Override
    public BillSplitMode mode() {
        return BillSplitMode.AMOUNT;
    }

    @Override
    public List<OrderBillDomain> split(OrderDomain order, List<OrderBillServicePort.SplitBillCommand> commands,
            UUID userId) {
        return commands.stream()
                .map(command -> {
                    var allocation = OrderBillAllocationDomain.builder()
                            .amount(scale(command.amount()))
                            .build();
                    return bill(order, command.label(), mode(), command.amount(), List.of(allocation), userId);
                })
                .toList();
    }
}
