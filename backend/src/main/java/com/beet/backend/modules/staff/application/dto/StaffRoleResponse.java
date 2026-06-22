package com.beet.backend.modules.staff.application.dto;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record StaffRoleResponse(
        UUID id,
        String name,
        Map<PermissionModule, List<PermissionAction>> permissions,
        String presetKey,
        boolean active,
        long assignedUsers) {
}
