package com.beet.backend.modules.supplier.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public class SupplierNotFoundException extends ResourceNotFoundException {

    public SupplierNotFoundException(String message) {
        super(message);
    }

    public static SupplierNotFoundException forId(UUID id) {
        return new SupplierNotFoundException("Supplier not found with id: " + id);
    }
}
