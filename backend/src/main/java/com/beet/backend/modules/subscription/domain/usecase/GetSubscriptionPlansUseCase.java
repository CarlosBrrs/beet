package com.beet.backend.modules.subscription.domain.usecase;

import com.beet.backend.modules.subscription.domain.api.GetSubscriptionPlansServicePort;
import com.beet.backend.modules.subscription.domain.model.SubscriptionPlan;
import com.beet.backend.modules.subscription.domain.spi.SubscriptionPersistencePort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSubscriptionPlansUseCase implements GetSubscriptionPlansServicePort {

    private final SubscriptionPersistencePort persistencePort;

    @Override
    public List<SubscriptionPlan> getPlans() {
        return persistencePort.findAll();
    }
}
