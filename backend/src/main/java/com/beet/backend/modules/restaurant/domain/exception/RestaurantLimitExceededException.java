package com.beet.backend.modules.restaurant.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceLimitExceededException;

public class RestaurantLimitExceededException extends ResourceLimitExceededException {

    private static final String LIMIT_REACHED_TEMPLATE = "Maximum number of restaurants (%d) reached for your plan.";

    private RestaurantLimitExceededException(String message) {
        super(message);
    }

    public static RestaurantLimitExceededException forPlan(int maxAllowed) {
        return new RestaurantLimitExceededException(String.format(LIMIT_REACHED_TEMPLATE, maxAllowed));
    }
}
