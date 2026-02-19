package com.beet.backend.modules.user.domain.constants;

public final class UserValidationConstants {

    private UserValidationConstants() {
    }

    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String EMAIL_INVALID = "Invalid email format";
    public static final String PASSWORD_REQUIRED = "Password is required";
    public static final String PASSWORD_MIN_LENGTH = "Password must be at least 8 characters";
    public static final String FIRST_NAME_REQUIRED = "First name is required";
    public static final String FIRST_LASTNAME_REQUIRED = "First lastname is required";
    public static final String PHONE_NUMBER_REQUIRED = "Phone number is required";
    public static final String USERNAME_REQUIRED = "Username is required";
    public static final String PLAN_ID_REQUIRED = "Subscription Plan ID is required";
}
