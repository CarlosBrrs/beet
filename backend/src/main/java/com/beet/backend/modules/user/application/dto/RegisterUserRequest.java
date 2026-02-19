package com.beet.backend.modules.user.application.dto;

import static com.beet.backend.modules.user.domain.constants.UserValidationConstants.EMAIL_INVALID;
import static com.beet.backend.modules.user.domain.constants.UserValidationConstants.EMAIL_REQUIRED;
import static com.beet.backend.modules.user.domain.constants.UserValidationConstants.FIRST_LASTNAME_REQUIRED;
import static com.beet.backend.modules.user.domain.constants.UserValidationConstants.FIRST_NAME_REQUIRED;
import static com.beet.backend.modules.user.domain.constants.UserValidationConstants.PASSWORD_MIN_LENGTH;
import static com.beet.backend.modules.user.domain.constants.UserValidationConstants.PASSWORD_REQUIRED;
import static com.beet.backend.modules.user.domain.constants.UserValidationConstants.PHONE_NUMBER_REQUIRED;
import static com.beet.backend.modules.user.domain.constants.UserValidationConstants.PLAN_ID_REQUIRED;
import static com.beet.backend.modules.user.domain.constants.UserValidationConstants.USERNAME_REQUIRED;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterUserRequest(
                @NotBlank(message = EMAIL_REQUIRED) @Email(message = EMAIL_INVALID) String email,

                @NotBlank(message = PASSWORD_REQUIRED) @Size(min = 8, message = PASSWORD_MIN_LENGTH) String password,

                @NotBlank(message = FIRST_NAME_REQUIRED) String firstName,

                String secondName,

                @NotBlank(message = FIRST_LASTNAME_REQUIRED) String firstLastname,

                String secondLastname,

                @NotBlank(message = PHONE_NUMBER_REQUIRED) String phoneNumber,

                @NotBlank(message = USERNAME_REQUIRED) String username,

                @NotNull(message = PLAN_ID_REQUIRED) UUID subscriptionPlanId) {
}
