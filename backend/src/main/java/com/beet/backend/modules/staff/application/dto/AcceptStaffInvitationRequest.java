package com.beet.backend.modules.staff.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptStaffInvitationRequest(
        @NotBlank String firstName,
        String secondName,
        @NotBlank String firstLastname,
        String secondLastname,
        String phoneNumber,
        String username,
        @NotBlank @Size(min = 8) String password) {
}
