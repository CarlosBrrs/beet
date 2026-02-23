package com.beet.backend.modules.documenttype.domain.exception;

public class InvalidDocumentTypeSearchException extends RuntimeException {

    private InvalidDocumentTypeSearchException(String message) {
        super(message);
    }

    public static InvalidDocumentTypeSearchException missingCountryCode() {
        return new InvalidDocumentTypeSearchException("Country code is required to search document types");
    }
}
