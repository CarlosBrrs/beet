package com.beet.backend.modules.staff.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StaffInvitationRequest(
        @NotBlank @Email String email,
        @NotNull UUID roleId) {
}
