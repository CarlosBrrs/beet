package com.beet.backend.modules.cash.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateCashRegisterRequest(
        @Size(min = 1, max = 120) String name,
        Boolean isActive,
        String notes) {
}
