package com.beet.backend.modules.table.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantTableRequest(
        @Size(min = 1, max = 120) String name,
        @Min(1) Integer capacity,
        @Size(max = 120) String area,
        @Min(0) Integer sortOrder,
        Boolean isActive,
        String notes) {
}
