package com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.role.domain.model.RoleDomain;
import com.beet.backend.modules.role.infrastructure.output.persistence.jdbc.aggregate.RoleAggregate;
import org.springframework.stereotype.Component;

@Component
public class RoleAggregateMapper {

    public RoleDomain toDomain(RoleAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }
        return RoleDomain.builder()
                .id(aggregate.id())
                .name(aggregate.name())
                .permissions(aggregate.permissions())
                .build();
    }

    public RoleAggregate toAggregate(RoleDomain domain) {
        if (domain == null) {
            return null;
        }
        return new RoleAggregate(
                domain.getId(),
                domain.getName(),
                domain.getPermissions());
    }
}
