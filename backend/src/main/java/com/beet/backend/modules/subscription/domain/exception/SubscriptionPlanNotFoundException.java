package com.beet.backend.modules.subscription.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;
import java.util.UUID;

public class SubscriptionPlanNotFoundException extends ResourceNotFoundException {
    public static final String PLAN_NOT_FOUND_TEMPLATE = "Subscription Plan not found: %s";

    private SubscriptionPlanNotFoundException(String template, UUID value) {
        super(String.format(template, value));
    }

    public static SubscriptionPlanNotFoundException forId(UUID id) {
        return new SubscriptionPlanNotFoundException(PLAN_NOT_FOUND_TEMPLATE, id);
    }
}
