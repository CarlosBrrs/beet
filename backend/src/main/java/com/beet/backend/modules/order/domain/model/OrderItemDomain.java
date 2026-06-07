package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderItemDomain {
    private UUID id;
    private UUID orderId;
    private OrderLineType lineType;
    private UUID itemId;
    private UUID templateId;
    private UUID submenuNodeId;
    private String itemNameSnapshot;
    private BigDecimal unitPriceSnapshot;
    private BigDecimal theoreticalCostSnapshot;
    private BigDecimal quantity;
    private BigDecimal subtotalGrossSnapshot;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    @Builder.Default
    private List<OrderItemTaxDomain> taxes = new ArrayList<>();

    @Builder.Default
    private List<OrderItemTemplateSlotDomain> templateSlots = new ArrayList<>();

    @Builder.Default
    private List<OrderItemIngredientRequirementDomain> ingredientRequirements = new ArrayList<>();
}
