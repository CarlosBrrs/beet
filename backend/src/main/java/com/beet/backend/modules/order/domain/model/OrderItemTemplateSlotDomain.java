package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderItemTemplateSlotDomain {
    private UUID id;
    private UUID orderItemId;
    private UUID templateSlotId;
    private String slotNameSnapshot;
    private int minSelectionSnapshot;
    private int maxSelectionSnapshot;
    private int sortOrder;

    @Builder.Default
    private List<OrderItemTemplateOptionDomain> options = new ArrayList<>();
}
