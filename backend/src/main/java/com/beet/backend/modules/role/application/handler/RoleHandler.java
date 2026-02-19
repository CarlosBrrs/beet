package com.beet.backend.modules.role.application.handler;

import com.beet.backend.modules.role.application.dto.RoleContextResponse;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import java.util.UUID;

public interface RoleHandler {
    ApiGenericResponse<RoleContextResponse> getPermissions(UUID restaurantId);
}
