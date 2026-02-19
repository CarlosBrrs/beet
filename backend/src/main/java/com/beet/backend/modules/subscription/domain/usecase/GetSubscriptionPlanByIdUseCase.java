package com.beet.backend.modules.subscription.domain.usecase;

import com.beet.backend.modules.subscription.domain.api.GetSubscriptionPlanByIdServicePort;
import com.beet.backend.modules.subscription.domain.model.SubscriptionPlan;
import com.beet.backend.modules.subscription.domain.spi.SubscriptionPersistencePort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

import com.beet.backend.modules.subscription.domain.exception.SubscriptionPlanNotFoundException;

@Service
@RequiredArgsConstructor
public class GetSubscriptionPlanByIdUseCase implements GetSubscriptionPlanByIdServicePort {

    private final SubscriptionPersistencePort persistencePort;

    @Override
    public SubscriptionPlan getPlanById(UUID id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> SubscriptionPlanNotFoundException.forId(id));
    }
}
