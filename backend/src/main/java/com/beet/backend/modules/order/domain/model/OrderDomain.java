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
public class OrderDomain {
    private UUID id;
    private UUID restaurantId;
    private UUID cashSessionId;
    private OrderStatus orderStatus;
    private KitchenStatus kitchenStatus;
    private PaymentStatus paymentStatus;
    private ServiceType serviceType;
    private UUID tableId;
    private String customerName;
    private boolean prepaymentRequiredSnapshot;
    private BigDecimal taxRateSnapshot;
    private BigDecimal subtotalGrossSnapshot;
    private BigDecimal taxAmountSnapshot;
    private BigDecimal totalGrossSnapshot;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    @Builder.Default
    private List<OrderItemDomain> items = new ArrayList<>();

    @Builder.Default
    private List<OrderTaxDomain> taxes = new ArrayList<>();
}
