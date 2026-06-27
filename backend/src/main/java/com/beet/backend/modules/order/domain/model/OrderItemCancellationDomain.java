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
public class OrderItemCancellationDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID orderId;
    private UUID orderItemId;
    private BigDecimal quantity;
    private BigDecimal grossAmount;
    private String reason;
    private KitchenStatus kitchenStatusSnapshot;
    private OrderItemInventoryDisposition inventoryDisposition;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private boolean systemGenerated;

    @Builder.Default
    private List<OrderItemCancellationInventoryDomain> inventoryEntries = new ArrayList<>();
}
