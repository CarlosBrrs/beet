package com.beet.backend.modules.order.domain.spi;

import com.beet.backend.modules.order.domain.model.BillSearchCriteria;
import com.beet.backend.modules.order.domain.model.OrderBillDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.util.List;
import java.util.UUID;

public interface OrderBillPersistencePort {
    void deleteBillsByOrder(UUID orderId);

    List<OrderBillDomain> saveBills(List<OrderBillDomain> bills);

    PageResponse<OrderBillDomain> findBills(BillSearchCriteria criteria);

    List<OrderBillDomain> findBillsByOrder(UUID restaurantId, UUID orderId);
}
