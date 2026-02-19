package com.beet.backend.modules.unit.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
public class UnitDomain {
    private final UUID id;
    private String name;
    private String abbreviation;
    private UnitType type;
    private BigDecimal factorToBase; // 1 for base units, conversion factor for derived
    private Boolean isBase;
}
