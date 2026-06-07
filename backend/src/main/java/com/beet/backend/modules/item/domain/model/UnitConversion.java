package com.beet.backend.modules.item.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record UnitConversion(
        UUID sourceUnitId,
        UUID baseUnitId,
        String baseUnitAbbreviation,
        BigDecimal factorToBase) {
}
