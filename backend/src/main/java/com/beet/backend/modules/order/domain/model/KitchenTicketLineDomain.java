package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class KitchenTicketLineDomain {
    private UUID id;
    private UUID kitchenTicketId;
    private UUID orderItemId;
    private OrderLineType lineType;
    private BigDecimal quantity;
    @Builder.Default
    private BigDecimal canceledQuantity = BigDecimal.ZERO;
    private String itemNameSnapshot;
    private String notes;

    @Builder.Default
    private List<OrderItemTemplateSlotDomain> templateSlots = new ArrayList<>();

    public BigDecimal getActiveQuantity() {
        BigDecimal original = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal canceled = canceledQuantity == null ? BigDecimal.ZERO : canceledQuantity;
        return original.subtract(canceled);
    }
}
