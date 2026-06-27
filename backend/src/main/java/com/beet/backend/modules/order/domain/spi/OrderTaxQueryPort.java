package com.beet.backend.modules.order.domain.spi;

import com.beet.backend.modules.order.domain.model.OrderTaxDefinition;

import java.util.List;
import java.util.UUID;

public interface OrderTaxQueryPort {
    List<OrderTaxDefinition> findActiveTaxesByRestaurant(UUID restaurantId);
}
