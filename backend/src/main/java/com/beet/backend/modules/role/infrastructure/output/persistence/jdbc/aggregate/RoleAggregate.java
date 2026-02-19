package com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("roles")
public record RoleAggregate(
        @Id UUID id,
        String name,
        Permissions permissions) {
}
