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
public class OrderItemConsumptionDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID ingredientStockId;
    private UUID masterIngredientId;
    private BigDecimal quantityBase;
    private BigDecimal unitCostSnapshot;
    private UUID inventoryTransactionId;
    private OffsetDateTime createdAt;
    private UUID createdBy;
}
