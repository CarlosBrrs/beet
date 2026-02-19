package com.beet.backend.modules.role.domain.model;

import lombok.Builder;
import lombok.Getter;

import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate.Permissions;

import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class RoleDomain {
    private final UUID id;
    private final String name;
    private final Permissions permissions;
}
