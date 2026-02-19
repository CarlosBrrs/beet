package com.beet.backend.modules.restaurant.infrastructure.output.persistence.jdbc.repository;

import com.beet.backend.modules.restaurant.infrastructure.output.persistence.jdbc.aggregate.RestaurantAggregate;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RestaurantJdbcRepository
        extends ListCrudRepository<RestaurantAggregate, UUID>, PagingAndSortingRepository<RestaurantAggregate, UUID> {

    List<RestaurantAggregate> findAllByOwnerId(UUID ownerId);

    int countByOwnerId(UUID ownerId);

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    boolean existsByAddressAndOwnerId(String address, UUID ownerId);

    boolean existsByPhoneNumberAndOwnerId(String phoneNumber, UUID ownerId);

}
