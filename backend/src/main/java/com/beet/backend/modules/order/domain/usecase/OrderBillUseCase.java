package com.beet.backend.modules.order.domain.usecase;

import com.beet.backend.modules.order.domain.api.OrderBillServicePort;
import com.beet.backend.modules.order.domain.exception.OrderNotFoundException;
import com.beet.backend.modules.order.domain.model.BillSearchCriteria;
import com.beet.backend.modules.order.domain.model.BillSplitMode;
import com.beet.backend.modules.order.domain.model.OrderBillDomain;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.spi.OrderBillPersistencePort;
import com.beet.backend.modules.order.domain.spi.OrderPersistencePort;
import com.beet.backend.modules.order.domain.strategy.BillSplitStrategyRegistry;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderBillUseCase implements OrderBillServicePort {

    private final OrderPersistencePort orderPersistence;
    private final OrderBillPersistencePort billPersistence;
    private final BillSplitStrategyRegistry registry;

    @Override
    @Transactional
    public List<OrderBillDomain> split(UUID restaurantId, UUID orderId, BillSplitMode mode,
            List<SplitBillCommand> bills, UUID userId) {
        OrderDomain order = orderPersistence.findByIdWithItems(orderId)
                .filter(candidate -> restaurantId.equals(candidate.getRestaurantId()))
                .orElseThrow(() -> OrderNotFoundException.forId(orderId));
        List<OrderBillDomain> split = registry.get(mode).split(order, bills, userId);
        billPersistence.deleteBillsByOrder(orderId);
        return billPersistence.saveBills(split);
    }

    @Override
    public PageResponse<OrderBillDomain> list(BillSearchCriteria criteria) {
        return billPersistence.findBills(criteria);
    }
}
