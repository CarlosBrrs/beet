package com.beet.backend.modules.order.domain.spi;

import java.util.UUID;

public interface OrderTableGateway {
    void validateAvailableForOrder(UUID restaurantId, UUID tableId);
}
