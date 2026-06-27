package com.beet.backend.modules.table.domain.spi;

import com.beet.backend.modules.table.domain.model.RestaurantTableDomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantTablePersistencePort {
    RestaurantTableDomain save(RestaurantTableDomain table);

    RestaurantTableDomain update(RestaurantTableDomain table);

    Optional<RestaurantTableDomain> findByIdForUpdate(UUID id);

    List<RestaurantTableDomain> findByRestaurantId(UUID restaurantId);

    boolean existsByName(UUID restaurantId, String name);

    Optional<UUID> findOpenOrderId(UUID restaurantId, UUID tableId);
}
