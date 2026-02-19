package com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.adapter;

import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.domain.spi.UserPersistencePort;
import com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.aggregate.UserAggregate;
import com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.mapper.UserAggregateMapper;
import com.beet.backend.modules.user.infrastructure.output.persistence.jdbc.repository.UserJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserJdbcAdapter implements UserPersistencePort {

    private final UserJdbcRepository repository;
    private final UserAggregateMapper mapper;

    @Override
    public User save(User user) {
        UserAggregate aggregate = mapper.toAggregate(user);
        UserAggregate saved = repository.save(aggregate);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return repository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsSubscriptionPlan(UUID subscriptionPlanId) {
        return repository.existsSubscriptionPlan(subscriptionPlanId);
    }
}
