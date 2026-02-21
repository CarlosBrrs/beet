package com.beet.backend.modules.ingredient.domain.exception;

import java.util.UUID;

public class UnitTypeMismatchException extends RuntimeException {

    public UnitTypeMismatchException(String message) {
        super(message);
    }

    public static UnitTypeMismatchException between(String baseType, String conversionType) {
        return new UnitTypeMismatchException(
                "Base unit type '" + baseType + "' does not match conversion unit type '" + conversionType
                        + "'. Both must be the same category (MASS↔MASS, VOLUME↔VOLUME).");
    }

    public static UnitTypeMismatchException unitNotFound(UUID unitId) {
        return new UnitTypeMismatchException("Unit not found with id: " + unitId);
    }
}
