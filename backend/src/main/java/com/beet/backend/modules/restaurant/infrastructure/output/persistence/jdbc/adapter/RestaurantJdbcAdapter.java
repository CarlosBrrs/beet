package com.beet.backend.modules.restaurant.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPersistencePort;
import com.beet.backend.modules.restaurant.infrastructure.output.persistence.jdbc.aggregate.RestaurantAggregate;
import com.beet.backend.modules.restaurant.infrastructure.output.persistence.jdbc.mapper.RestaurantAggregateMapper;
import com.beet.backend.modules.restaurant.infrastructure.output.persistence.jdbc.repository.RestaurantJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class RestaurantJdbcAdapter implements RestaurantPersistencePort {

    private final RestaurantJdbcRepository repository;
    private final RestaurantAggregateMapper mapper;

    @Override
    public RestaurantDomain save(RestaurantDomain domain) {
        RestaurantAggregate aggregate;
        if (domain.getId() != null) {
            aggregate = repository.findById(domain.getId())
                    .map(existing -> {
                        mapper.updateAggregate(existing, domain);
                        return existing;
                    })
                    .orElseGet(() -> mapper.toAggregate(domain));
        } else {
            aggregate = mapper.toAggregate(domain);
        }
        RestaurantAggregate saved = repository.save(aggregate);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RestaurantDomain> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public List<RestaurantDomain> findAllByOwnerId(UUID ownerId) {
        return repository.findAllByOwnerId(ownerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RestaurantDomain> findAllById(List<UUID> ids) {
        return StreamSupport.stream(repository.findAllById(ids).spliterator(), false)
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int countByOwnerId(UUID ownerId) {
        return repository.countByOwnerId(ownerId);
    }

    @Override
    public boolean existsByNameAndOwnerId(String name, UUID ownerId) {
        return repository.existsByNameAndOwnerId(name, ownerId);
    }

    @Override
    public boolean existsByAddressAndOwnerId(String address, UUID ownerId) {
        return repository.existsByAddressAndOwnerId(address, ownerId);
    }

    @Override
    public boolean existsByPhoneNumberAndOwnerId(String phoneNumber, UUID ownerId) {
        return repository.existsByPhoneNumberAndOwnerId(phoneNumber, ownerId);
    }

}
