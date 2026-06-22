package com.beet.backend.modules.staff.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StaffMemberResponse(
        UUID userId,
        String email,
        String fullName,
        String accountStatus,
        Instant lastLoginAt,
        List<Assignment> assignments) {

    public record Assignment(
            UUID restaurantId,
            String restaurantName,
            UUID roleId,
            String roleName,
            String assignmentStatus) {
    }
}
