package com.beet.backend.modules.staff.application.dto;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;

import java.util.List;

public record PermissionCatalogResponse(
        PermissionModule module,
        String label,
        List<PermissionAction> actions) {
}
