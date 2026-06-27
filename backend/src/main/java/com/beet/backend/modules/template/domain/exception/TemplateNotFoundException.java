package com.beet.backend.modules.template.domain.exception;

import java.util.UUID;

public class TemplateNotFoundException extends RuntimeException {
    private TemplateNotFoundException(String message) {
        super(message);
    }

    public static TemplateNotFoundException forId(UUID id) {
        return new TemplateNotFoundException("Template not found with id: " + id);
    }
}
