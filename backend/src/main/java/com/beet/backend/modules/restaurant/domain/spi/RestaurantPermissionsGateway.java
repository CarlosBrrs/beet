package com.beet.backend.modules.restaurant.domain.spi;

import com.beet.backend.modules.restaurant.domain.model.UserRestaurantPermissions;

import java.util.UUID;

public interface RestaurantPermissionsGateway {
    UserRestaurantPermissions getMyPermissionsForRestaurant(UUID restaurantId, UUID userId);
}
