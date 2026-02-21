package com.beet.backend.modules.supplier.domain.exception;

import com.beet.backend.shared.domain.exception.ResourceAlreadyExistsException;

public class SupplierAlreadyExistsException extends ResourceAlreadyExistsException {

    public SupplierAlreadyExistsException(String message) {
        super(message);
    }

    public static SupplierAlreadyExistsException forDocument(String documentNumber) {
        return new SupplierAlreadyExistsException(
                "A supplier with document number '" + documentNumber + "' already exists for this owner");
    }
}
