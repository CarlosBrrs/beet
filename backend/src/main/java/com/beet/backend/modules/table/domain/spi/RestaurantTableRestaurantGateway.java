package com.beet.backend.modules.table.domain.spi;

import java.util.UUID;

public interface RestaurantTableRestaurantGateway {
    Integer getMaxTableCapacity(UUID restaurantId);
}
