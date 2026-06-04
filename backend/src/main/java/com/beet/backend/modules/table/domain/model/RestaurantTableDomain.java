package com.beet.backend.modules.table.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class RestaurantTableDomain {
    private UUID id;
    private UUID restaurantId;
    private String name;
    private Integer capacity;
    private String area;
    private Integer sortOrder;
    private Boolean isActive;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private TableAvailabilityStatus availabilityStatus;
    private UUID openOrderId;
}
