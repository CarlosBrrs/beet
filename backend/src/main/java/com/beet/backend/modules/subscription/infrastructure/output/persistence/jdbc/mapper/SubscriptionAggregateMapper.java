package com.beet.backend.modules.subscription.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.subscription.domain.model.SubscriptionFeatures;
import com.beet.backend.modules.subscription.domain.model.SubscriptionPlan;
import com.beet.backend.modules.subscription.infrastructure.output.persistence.jdbc.aggregate.SubscriptionAggregateFeatures;
import com.beet.backend.modules.subscription.infrastructure.output.persistence.jdbc.aggregate.SubscriptionPlanAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionAggregateMapper {

    public SubscriptionPlan toDomain(SubscriptionPlanAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }
        return SubscriptionPlan.builder()
                .id(aggregate.getId())
                .name(aggregate.getName())
                .description(aggregate.getDescription())
                .price(aggregate.getPrice())
                .currency(aggregate.getCurrency())
                .interval(aggregate.getBillingCycle())
                .features(toFeaturesDomain(aggregate.getFeatures()))
                .build();
    }

    private SubscriptionFeatures toFeaturesDomain(
            SubscriptionAggregateFeatures aggregateFeatures) {
        if (aggregateFeatures == null) {
            return null;
        }
        return SubscriptionFeatures.builder()
                .maxRestaurants(aggregateFeatures.getMaxRestaurants())
                .maxEmployees(aggregateFeatures.getMaxEmployees())
                .advancedReporting(aggregateFeatures.isAdvancedReporting())
                .prioritySupport(aggregateFeatures.isPrioritySupport())
                .multiUserAccess(aggregateFeatures.isMultiUserAccess())
                .build();
    }
}
