package com.beet.backend.modules.restaurant.domain.spi;

import java.util.UUID;

public interface RestaurantSubscriptionGateway {
    int getMaxRestaurantsAllowed(UUID ownerId);
}
