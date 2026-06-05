package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderBillAllocationDomain {
    private UUID id;
    private UUID orderBillId;
    private UUID orderItemId;
    private BigDecimal quantity;
    private BigDecimal amount;
}
