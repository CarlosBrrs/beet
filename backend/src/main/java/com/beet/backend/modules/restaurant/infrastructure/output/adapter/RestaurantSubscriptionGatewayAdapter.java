package com.beet.backend.modules.restaurant.infrastructure.output.adapter; // Note: simplified package for adapter

import com.beet.backend.modules.restaurant.domain.spi.RestaurantSubscriptionGateway;
import com.beet.backend.modules.subscription.domain.api.GetSubscriptionPlanByIdServicePort;
import com.beet.backend.modules.subscription.domain.model.SubscriptionPlan;
import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.domain.spi.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RestaurantSubscriptionGatewayAdapter implements RestaurantSubscriptionGateway {

    private final UserPersistencePort userPersistencePort;
    private final GetSubscriptionPlanByIdServicePort subscriptionServicePort;

    @Override
    public int getMaxRestaurantsAllowed(UUID ownerId) {
        Optional<User> byId = userPersistencePort.findById(ownerId);
        Optional<UUID> map = byId
                .map(User::getSubscriptionPlanId);
        Optional<SubscriptionPlan> map2 = map
                .map(subscriptionServicePort::getPlanById);
        Optional<Integer> map3 = map2
                .map(plan -> plan.getFeatures().getMaxRestaurants());
        Integer orElse = map3
                .orElse(1);
        return orElse; // Default to 1 if not found
    }
}
