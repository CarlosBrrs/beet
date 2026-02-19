package com.beet.backend.modules.subscription.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.subscription.domain.model.SubscriptionPlan;
import com.beet.backend.modules.subscription.domain.spi.SubscriptionPersistencePort;
import com.beet.backend.modules.subscription.infrastructure.output.persistence.jdbc.mapper.SubscriptionAggregateMapper;
import com.beet.backend.modules.subscription.infrastructure.output.persistence.jdbc.repository.SubscriptionPlanJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class SubscriptionJdbcAdapter implements SubscriptionPersistencePort {

    private final SubscriptionPlanJdbcRepository repository;
    private final SubscriptionAggregateMapper mapper;

    @Override
    public List<SubscriptionPlan> findAll() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<SubscriptionPlan> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
}
