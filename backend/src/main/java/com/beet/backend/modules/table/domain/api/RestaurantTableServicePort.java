package com.beet.backend.modules.table.domain.api;

import com.beet.backend.modules.table.domain.model.RestaurantTableDomain;

import java.util.List;
import java.util.UUID;

public interface RestaurantTableServicePort {
    RestaurantTableDomain create(RestaurantTableDomain table);

    RestaurantTableDomain update(RestaurantTableDomain table);

    RestaurantTableDomain deactivate(UUID restaurantId, UUID tableId, UUID updatedBy);

    List<RestaurantTableDomain> listByRestaurant(UUID restaurantId);

    void validateAvailableForOrder(UUID restaurantId, UUID tableId);
}
