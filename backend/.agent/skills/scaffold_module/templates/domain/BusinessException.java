package com.beet.backend.modules.{moduleName}.domain.exception;

public class {Aggregate}BusinessException extends RuntimeException {

    // Define specific error templates
    private static final String GENERIC_ERROR_TEMPLATE = "Error processing {Aggregate}: %s";

    private {Aggregate}BusinessException(String message) {
        super(message);
    }
    
    // Factory methods for specific scenarios
    public static {Aggregate}BusinessException withMessage(String detail) {
        return new {Aggregate}BusinessException(String.format(GENERIC_ERROR_TEMPLATE, detail));
    }
    
    // Example: DateRangeInvalid
    public static {Aggregate}BusinessException dateRangeInvalid(String startDate, String endDate) {
        return new {Aggregate}BusinessException(String.format("Invalid date range for {Aggregate}: %s to %s", startDate, endDate));
    }
}
