package com.beet.backend.modules.subscription.infrastructure.output.persistence.jdbc.aggregate;

import com.beet.backend.modules.subscription.domain.model.BillingCycle;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@EqualsAndHashCode
@Table("subscription_plans")
public class SubscriptionPlanAggregate {
    @Id
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private BillingCycle billingCycle; // mapped to varchar or enum in DB
    private SubscriptionAggregateFeatures features; // JSON string in DB mapped via Converter
}
