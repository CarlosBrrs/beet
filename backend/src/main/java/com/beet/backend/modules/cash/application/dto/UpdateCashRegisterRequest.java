package com.beet.backend.modules.cash.application.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCashRegisterRequest(
        @Size(min = 1, max = 120) String name,
        UUID deviceId,
        Boolean isActive,
        String notes) {
}
