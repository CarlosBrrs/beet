package com.beet.backend.modules.order.domain.strategy;

import com.beet.backend.modules.order.domain.api.OrderBillServicePort;
import com.beet.backend.modules.order.domain.model.BillSplitMode;
import com.beet.backend.modules.order.domain.model.OrderBillDomain;
import com.beet.backend.modules.order.domain.model.OrderDomain;

import java.util.List;
import java.util.UUID;

public interface BillSplitStrategy {
    BillSplitMode mode();

    List<OrderBillDomain> split(OrderDomain order, List<OrderBillServicePort.SplitBillCommand> commands, UUID userId);
}
