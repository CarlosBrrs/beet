package com.beet.backend.modules.subscription.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionPlan {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency; // ISO code (e.g., USD)
    private BillingCycle interval;
    private SubscriptionFeatures features;
}
