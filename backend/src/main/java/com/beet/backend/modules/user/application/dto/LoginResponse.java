package com.beet.backend.modules.user.application.dto;

import lombok.Builder;

@Builder
public record LoginResponse(String token, UserResponse user) {
}
