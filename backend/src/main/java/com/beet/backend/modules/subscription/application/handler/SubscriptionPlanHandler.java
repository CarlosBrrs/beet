package com.beet.backend.modules.subscription.application.handler;

import com.beet.backend.modules.subscription.application.dto.SubscriptionPlanResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface SubscriptionPlanHandler {
    ResponseEntity<ApiGenericResponse<List<SubscriptionPlanResponse>>> getPlans();

    ResponseEntity<ApiGenericResponse<SubscriptionPlanResponse>> getPlanById(java.util.UUID id);
}
