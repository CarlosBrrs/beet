package com.beet.backend.modules.order.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderTaxDefinition(UUID id, String name, BigDecimal rate) {
}
