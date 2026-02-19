package com.beet.backend.modules.role.application.handler;

import com.beet.backend.modules.role.application.dto.RoleContextResponse;
import com.beet.backend.modules.role.application.mapper.RoleContextMapper;
import com.beet.backend.modules.role.domain.model.RoleDomain;
import com.beet.backend.modules.role.domain.usecase.GetRoleContextUseCase;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoleHandlerImpl implements RoleHandler {

    private final GetRoleContextUseCase getRoleContextUseCase;
    private final RoleContextMapper mapper;

    @Override
    public ApiGenericResponse<RoleContextResponse> getPermissions(UUID restaurantId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        RoleDomain domain = getRoleContextUseCase.invoke(userId,
                restaurantId);
        return ApiGenericResponse.success(mapper.toResponse(domain));
    }
}
