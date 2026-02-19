package com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.mapper;

import com.beet.backend.modules.unit.domain.model.UnitDomain;
import com.beet.backend.modules.unit.infrastructure.output.persistence.jdbc.aggregate.UnitConversionProjection;
import org.springframework.stereotype.Component;

@Component
public class UnitAggregateMapper {

    public UnitDomain toDomain(UnitConversionProjection projection) {
        if (projection == null)
            return null;
        return UnitDomain.builder()
                .id(projection.getId())
                .name(projection.getName())
                .abbreviation(projection.getAbbreviation())
                .type(projection.getType())
                .isBase(projection.getIsBase())
                .factorToBase(projection.getFactorToBase())
                .build();
    }
}
