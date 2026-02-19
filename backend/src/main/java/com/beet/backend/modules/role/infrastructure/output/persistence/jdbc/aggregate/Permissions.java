package com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NoArgsConstructor;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class Permissions {

    private Map<PermissionModule, List<PermissionAction>> modulePermissions;

    @JsonCreator
    public Permissions(Map<PermissionModule, List<PermissionAction>> modulePermissions) {
        this.modulePermissions = modulePermissions;
    }

    @JsonValue
    public Map<PermissionModule, List<PermissionAction>> getModulePermissions() {
        return modulePermissions;
    }
}
