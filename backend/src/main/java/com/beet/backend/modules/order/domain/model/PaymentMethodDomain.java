package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PaymentMethodDomain {
    private UUID id;
    private UUID restaurantId;
    private String code;
    private String name;
    private PaymentMethodType type;
    private boolean isActive;
    private boolean requiresReference;
    private int sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
