package com.beet.backend.modules.template.domain.exception;

public class TemplateAlreadyExistsException extends RuntimeException {
    private TemplateAlreadyExistsException(String message) {
        super(message);
    }

    public static TemplateAlreadyExistsException forName(String name) {
        return new TemplateAlreadyExistsException("A template with this name already exists: " + name);
    }
}
