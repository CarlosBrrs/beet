package com.beet.backend.modules.supplier.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SupplierRequest(
        @NotBlank(message = "Supplier name is required") String name,
        @NotNull(message = "Document type is required") UUID documentTypeId,
        @NotBlank(message = "Document number is required") String documentNumber,
        String contactName,
        @Email(message = "Email must be valid") String email,
        String phone,
        String address) {
}
