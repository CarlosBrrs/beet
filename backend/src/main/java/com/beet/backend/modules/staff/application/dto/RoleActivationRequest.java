package com.beet.backend.modules.staff.application.dto;

import jakarta.validation.constraints.NotNull;

public record RoleActivationRequest(@NotNull Boolean active) {
}
