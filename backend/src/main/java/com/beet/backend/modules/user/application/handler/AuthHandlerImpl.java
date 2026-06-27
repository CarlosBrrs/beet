package com.beet.backend.modules.user.application.handler;

import com.beet.backend.modules.role.application.dto.UserPermissionEntry;
import com.beet.backend.modules.role.domain.spi.RolePersistencePort;
import com.beet.backend.modules.user.application.dto.LoginRequest;
import com.beet.backend.modules.user.application.dto.LoginResponse;
import com.beet.backend.modules.user.application.dto.RegisterUserRequest;
import com.beet.backend.modules.user.application.dto.UserResponse;
import com.beet.backend.modules.user.application.mapper.UserServiceMapper;
import com.beet.backend.modules.user.domain.api.LoginUserServicePort;
import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.domain.usecase.RegisterUserUseCase;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthHandlerImpl implements AuthHandler {

    private final UserServiceMapper userServiceMapper;
    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserServicePort loginUserUseCase;
    private final RolePersistencePort rolePersistencePort;

    @Override
    public ApiGenericResponse<UserResponse> register(RegisterUserRequest request) {
        User user = userServiceMapper.toDomain(request);
        User savedUser = registerUserUseCase.register(user);
        return ApiGenericResponse.success(userServiceMapper.toResponse(savedUser));
    }

    @Override
    public ApiGenericResponse<LoginResponse> login(LoginRequest request) {
        LoginResponse response = loginUserUseCase.login(request.email(), request.password());
        return ApiGenericResponse.success(response);
    }

    @Override
    public ApiGenericResponse<List<UserPermissionEntry>> getMyPermissions(UUID userId) {
        List<UserPermissionEntry> permissions = rolePersistencePort.findAllPermissionsForUser(userId);
        return ApiGenericResponse.success(permissions);
    }
}
