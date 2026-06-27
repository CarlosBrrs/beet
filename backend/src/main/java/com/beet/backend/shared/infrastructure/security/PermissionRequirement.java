package com.beet.backend.shared.infrastructure.security;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;

public @interface PermissionRequirement {
    PermissionModule module();

    PermissionAction action();
}
