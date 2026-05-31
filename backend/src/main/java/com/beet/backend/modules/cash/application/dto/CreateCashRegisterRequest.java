package com.beet.backend.modules.cash.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCashRegisterRequest(
        @NotBlank @Size(max = 120) String name,
        UUID deviceId,
        String notes) {
}
