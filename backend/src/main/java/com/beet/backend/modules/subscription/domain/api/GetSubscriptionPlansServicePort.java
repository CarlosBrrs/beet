package com.beet.backend.modules.subscription.domain.api;

import com.beet.backend.modules.subscription.domain.model.SubscriptionPlan;
import java.util.List;

public interface GetSubscriptionPlansServicePort {
    List<SubscriptionPlan> getPlans();
}
