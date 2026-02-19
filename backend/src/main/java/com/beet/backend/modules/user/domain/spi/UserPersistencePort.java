package com.beet.backend.modules.user.domain.spi;

import com.beet.backend.modules.user.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserPersistencePort {
    User save(User user);

    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsSubscriptionPlan(UUID subscriptionPlanId);
}
