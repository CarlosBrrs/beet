package com.beet.backend.modules.restaurant.domain.spi;

import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantPersistencePort {
    RestaurantDomain save(RestaurantDomain domain);

    Optional<RestaurantDomain> findById(UUID id);

    boolean existsById(UUID id);

    List<RestaurantDomain> findAllByOwnerId(UUID ownerId);

    List<RestaurantDomain> findAllById(List<UUID> ids);

    int countByOwnerId(UUID ownerId);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    boolean existsByAddressAndOwnerId(String address, UUID ownerId);

    boolean existsByPhoneNumberAndOwnerId(String phoneNumber, UUID ownerId);

}
