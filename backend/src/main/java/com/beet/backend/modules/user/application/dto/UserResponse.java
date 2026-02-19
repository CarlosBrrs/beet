package com.beet.backend.modules.user.application.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserResponse(UUID id, String email, String fullName, String username, UUID subscriptionPlanId) {

}
