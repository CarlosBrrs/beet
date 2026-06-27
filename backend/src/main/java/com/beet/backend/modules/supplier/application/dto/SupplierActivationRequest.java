package com.beet.backend.modules.supplier.application.dto;

import jakarta.validation.constraints.NotNull;

public record SupplierActivationRequest(@NotNull Boolean isActive) {
}
