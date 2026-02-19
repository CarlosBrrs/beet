package com.beet.backend.modules.user.application.dto;

import static com.beet.backend.modules.user.domain.constants.UserValidationConstants.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LoginRequest(

    @NotBlank(message = EMAIL_REQUIRED)
    @Email(message = EMAIL_INVALID)
    String email,

    @NotBlank(message = PASSWORD_REQUIRED)
    String password) {
}
