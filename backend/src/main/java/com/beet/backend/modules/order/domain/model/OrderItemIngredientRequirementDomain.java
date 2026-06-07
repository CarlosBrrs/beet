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
public class OrderItemIngredientRequirementDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID masterIngredientId;
    private String ingredientNameSnapshot;
    private BigDecimal quantityBasePerSaleUnit;
    private BigDecimal unitCostSnapshot;
    private OffsetDateTime createdAt;
}
