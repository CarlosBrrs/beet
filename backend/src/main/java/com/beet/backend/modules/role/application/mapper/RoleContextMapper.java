package com.beet.backend.modules.role.application.mapper;

import com.beet.backend.modules.role.application.dto.RoleContextResponse;
import com.beet.backend.modules.role.domain.model.RoleDomain;
import org.springframework.stereotype.Component;

@Component
public class RoleContextMapper {

    public RoleContextResponse toResponse(RoleDomain domain) {
        if (domain == null) {
            return null;
        }
        return new RoleContextResponse(
                domain.getId(),
                domain.getName(),
                domain.getPermissions());
    }
}
