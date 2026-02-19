package com.beet.backend.modules.subscription.application.dto;

import com.beet.backend.modules.subscription.domain.model.BillingCycle;
import lombok.Builder;

import java.math.BigDecimal;

import java.util.UUID;

@Builder
public record SubscriptionPlanResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String currency, // ISO code
        BillingCycle interval,
        SubscriptionPlanFeaturesResponse features) {
}
