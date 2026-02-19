package com.beet.backend.modules.restaurant.domain.api;

import com.beet.backend.modules.restaurant.domain.model.UserRestaurantPermissions;

import java.util.UUID;

public interface RestaurantPermissionsServicePort {
    UserRestaurantPermissions getMyPermissionsForRestaurant(UUID restaurantId, UUID userId);
}
