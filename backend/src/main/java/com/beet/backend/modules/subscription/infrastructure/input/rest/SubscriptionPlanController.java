package com.beet.backend.modules.subscription.infrastructure.input.rest;

import com.beet.backend.modules.subscription.application.dto.SubscriptionPlanResponse;
import com.beet.backend.modules.subscription.application.handler.SubscriptionPlanHandler;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanHandler handler;

    @GetMapping("/plans")
    public ResponseEntity<ApiGenericResponse<List<SubscriptionPlanResponse>>> getPlans() {
        return handler.getPlans();
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<ApiGenericResponse<SubscriptionPlanResponse>> getPlanById(
            @PathVariable UUID id) {
        return handler.getPlanById(id);
    }
}
