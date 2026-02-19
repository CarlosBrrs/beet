package com.beet.backend.modules.subscription.application.handler;

import com.beet.backend.modules.subscription.application.dto.SubscriptionPlanResponse;
import com.beet.backend.modules.subscription.domain.api.GetSubscriptionPlanByIdServicePort;
import com.beet.backend.modules.subscription.domain.api.GetSubscriptionPlansServicePort;
import com.beet.backend.modules.subscription.application.mapper.SubscriptionMapper;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanHandlerImpl implements SubscriptionPlanHandler {

    private final GetSubscriptionPlansServicePort servicePort;
    private final SubscriptionMapper mapper;
    private final GetSubscriptionPlanByIdServicePort getByIdServicePort;

    @Override
    public ResponseEntity<ApiGenericResponse<List<SubscriptionPlanResponse>>> getPlans() {
        List<SubscriptionPlanResponse> response = servicePort.getPlans().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiGenericResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiGenericResponse<SubscriptionPlanResponse>> getPlanById(java.util.UUID id) {
        var plan = getByIdServicePort.getPlanById(id);
        var response = mapper.toResponse(plan);
        return ResponseEntity.ok(ApiGenericResponse.success(response));
    }
}
