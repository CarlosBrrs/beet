package com.beet.backend.modules.subscription.application.mapper;

import com.beet.backend.modules.subscription.application.dto.SubscriptionPlanFeaturesResponse;
import com.beet.backend.modules.subscription.application.dto.SubscriptionPlanResponse;
import com.beet.backend.modules.subscription.domain.model.SubscriptionFeatures;
import com.beet.backend.modules.subscription.domain.model.SubscriptionPlan;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {
    public SubscriptionPlanResponse toResponse(SubscriptionPlan domain) {
        if (domain == null) {
            return null;
        }
        return SubscriptionPlanResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .price(domain.getPrice())
                .currency(domain.getCurrency())
                .interval(domain.getInterval())
                .features(toFeaturesDto(domain.getFeatures()))
                .build();
    }

    private SubscriptionPlanFeaturesResponse toFeaturesDto(
            SubscriptionFeatures features) {
        if (features == null)
            return null;
        return SubscriptionPlanFeaturesResponse.builder()
                .maxRestaurants(features.getMaxRestaurants())
                .maxEmployees(features.getMaxEmployees())
                .advancedReporting(features.isAdvancedReporting())
                .prioritySupport(features.isPrioritySupport())
                .multiUserAccess(features.isMultiUserAccess())
                .build();
    }
}
