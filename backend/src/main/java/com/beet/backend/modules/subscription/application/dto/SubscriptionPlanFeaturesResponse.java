package com.beet.backend.modules.subscription.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionPlanFeaturesResponse {
    private int maxRestaurants;
    private int maxEmployees;
    private boolean advancedReporting;
    private boolean prioritySupport;
    private boolean multiUserAccess;
}
