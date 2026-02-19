package com.beet.backend.modules.restaurant.domain.api;

import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.model.RestaurantWithRole;

import java.util.List;
import java.util.UUID;

public interface RestaurantServicePort {
    RestaurantWithRole create(RestaurantDomain domain);

    RestaurantDomain getById(UUID id, UUID ownerId);

    RestaurantWithRole getByIdWithRole(UUID id, UUID userId);

    List<RestaurantDomain> getRestaurantsByOwner(UUID ownerId);

    List<RestaurantWithRole> getRestaurantsWithRole(UUID userId);

    boolean existsById(UUID id);

    RestaurantDomain update(RestaurantDomain domain);

    RestaurantWithRole updateWithRole(RestaurantDomain domain, UUID userId);
}
