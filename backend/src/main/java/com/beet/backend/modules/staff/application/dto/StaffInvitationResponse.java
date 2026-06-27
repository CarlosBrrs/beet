package com.beet.backend.modules.staff.application.dto;

import java.time.Instant;
import java.util.UUID;

public record StaffInvitationResponse(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        UUID roleId,
        String roleName,
        String email,
        String status,
        Instant expiresAt,
        Instant acceptedAt,
        Instant revokedAt,
        String invitationPath,
        Instant createdAt) {
}
