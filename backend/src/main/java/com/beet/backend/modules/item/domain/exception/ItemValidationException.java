package com.beet.backend.modules.item.domain.exception;

public class ItemValidationException extends RuntimeException {

    private ItemValidationException(String message) {
        super(message);
    }

    public static ItemValidationException flatProductCannotHaveRecipe() {
        return new ItemValidationException("Flat products cannot include recipe lines.");
    }

    public static ItemValidationException trackedProductRequiresRecipe() {
        return new ItemValidationException("Tracked products must include at least one recipe line.");
    }

    public static ItemValidationException trackedProductRequiresYield() {
        return new ItemValidationException("Tracked products must include yield quantity and yield unit.");
    }

    public static ItemValidationException defaultUnitNotFound(String abbreviation) {
        return new ItemValidationException("Default unit not found: " + abbreviation);
    }
}
