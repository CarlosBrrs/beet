package com.beet.backend.modules.cash.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RebindCashSessionRequest(
        @NotNull UUID newDeviceId) {
}
