package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderItemTaxDomain {
    private UUID id;
    private UUID orderItemId;
    private UUID taxId;
    private String taxNameSnapshot;
    private BigDecimal taxRateSnapshot;
    private BigDecimal taxBaseSnapshot;
    private BigDecimal taxAmountSnapshot;
    private OffsetDateTime createdAt;
    private UUID createdBy;
}
