package com.beet.backend.modules.unit.application.mapper;

import com.beet.backend.modules.unit.application.dto.UnitResponse;
import com.beet.backend.modules.unit.domain.model.UnitDomain;
import org.springframework.stereotype.Component;

@Component
public class UnitServiceMapper {

    public UnitResponse toResponse(UnitDomain domain) {
        return UnitResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .abbreviation(domain.getAbbreviation())
                .type(domain.getType().name()) // Enum → String
                .factorToBase(domain.getFactorToBase())
                .isBase(domain.getIsBase())
                .build();
    }
}
