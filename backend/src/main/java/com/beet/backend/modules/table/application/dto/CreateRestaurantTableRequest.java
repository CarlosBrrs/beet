package com.beet.backend.modules.table.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRestaurantTableRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Min(1) Integer capacity,
        @Size(max = 120) String area,
        @Min(0) Integer sortOrder,
        String notes) {
}
