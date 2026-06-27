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
public class InventoryReservationDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID ingredientStockId;
    private UUID masterIngredientId;
    private BigDecimal quantityBase;
    private BigDecimal unitCostSnapshot;
    private InventoryReservationStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
