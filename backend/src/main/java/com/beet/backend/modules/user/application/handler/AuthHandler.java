package com.beet.backend.modules.user.application.handler;

import com.beet.backend.modules.user.application.dto.RegisterUserRequest;
import com.beet.backend.modules.user.application.dto.UserResponse;
import com.beet.backend.modules.role.application.dto.UserPermissionEntry;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import com.beet.backend.modules.user.application.dto.LoginRequest;
import com.beet.backend.modules.user.application.dto.LoginResponse;

import java.util.List;
import java.util.UUID;

public interface AuthHandler {
    ApiGenericResponse<UserResponse> register(RegisterUserRequest request);

    ApiGenericResponse<LoginResponse> login(LoginRequest request);

    ApiGenericResponse<List<UserPermissionEntry>> getMyPermissions(UUID userId);
}
