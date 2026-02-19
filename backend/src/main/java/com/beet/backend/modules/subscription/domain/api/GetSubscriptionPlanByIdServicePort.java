package com.beet.backend.modules.subscription.domain.api;

import com.beet.backend.modules.subscription.domain.model.SubscriptionPlan;
import java.util.UUID;

public interface GetSubscriptionPlanByIdServicePort {
    SubscriptionPlan getPlanById(UUID id);
}
