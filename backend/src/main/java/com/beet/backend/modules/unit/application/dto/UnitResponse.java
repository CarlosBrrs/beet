package com.beet.backend.modules.unit.application.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record UnitResponse(
                UUID id,
                String name,
                String abbreviation,
                String type,
                BigDecimal factorToBase,
                Boolean isBase) {
}
