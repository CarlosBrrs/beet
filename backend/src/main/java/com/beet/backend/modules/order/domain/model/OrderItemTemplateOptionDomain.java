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
public class OrderItemTemplateOptionDomain {
    private UUID id;
    private UUID orderItemTemplateSlotId;
    private UUID slotOptionId;
    private UUID itemId;
    private String itemNameSnapshot;
    private BigDecimal quantity;
    private BigDecimal surchargeSnapshot;
    private BigDecimal theoreticalCostSnapshot;
}
