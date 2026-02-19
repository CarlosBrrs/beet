package com.beet.backend.modules.subscription.domain.spi;

import com.beet.backend.modules.subscription.domain.model.SubscriptionPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPersistencePort {
    List<SubscriptionPlan> findAll();

    Optional<SubscriptionPlan> findById(UUID id);
}
