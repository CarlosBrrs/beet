package com.beet.backend.modules.item.application.dto;

import jakarta.validation.constraints.NotNull;

public record ActivationRequest(@NotNull Boolean isActive) {}
