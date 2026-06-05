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
public class KitchenTicketLineDomain {
    private UUID id;
    private UUID kitchenTicketId;
    private UUID orderItemId;
    private BigDecimal quantity;
    private String itemNameSnapshot;
    private String notes;
}
