package com.beet.backend.modules.order.domain.spi;

import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTaxDomain;
import com.beet.backend.modules.order.domain.model.OrderTaxDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderPersistencePort {

    OrderDomain save(OrderDomain order);

    OrderDomain update(OrderDomain order);

    OrderItemDomain saveItem(OrderItemDomain item);

    void updateItemQuantity(UUID orderItemId, BigDecimal quantity, BigDecimal subtotalGrossSnapshot, UUID updatedBy);

    void deleteItem(UUID orderItemId);

    void replaceOrderTaxes(UUID orderId, List<OrderTaxDomain> taxes);

    void replaceOrderItemTaxes(UUID orderId, List<OrderItemTaxDomain> taxes);

    Optional<OrderDomain> findByIdWithItems(UUID orderId);

    PageResponse<OrderDomain> findAllPaged(UUID restaurantId, int page, int size, String search);
}
