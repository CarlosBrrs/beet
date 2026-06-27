package com.beet.backend.modules.staff.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StaffAssignmentRequest(
        UUID roleId,
        @NotNull String status) {
}
