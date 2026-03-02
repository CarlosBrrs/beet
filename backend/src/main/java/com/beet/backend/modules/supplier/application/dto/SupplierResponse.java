package com.beet.backend.modules.supplier.application.dto;

import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        UUID documentTypeId,
        String documentNumber,
        String contactName,
        String email,
        String phone,
        String address,
        boolean isActive) {
}
