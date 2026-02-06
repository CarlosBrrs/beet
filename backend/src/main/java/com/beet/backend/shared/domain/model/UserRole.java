package com.beet.backend.shared.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    SUPER_ADMIN("Super Admin"),
    OWNER("Owner"),
    BRANCH_MANAGER("Branch Manager"),
    CHEF("Chef"),
    COOK("Cook"),
    CASHIER("Cashier"),
    WAITER("Waiter");

    private final String dbName;

    public static UserRole fromDbName(String name) {
        for (UserRole role : values()) {
            if (role.dbName.equalsIgnoreCase(name)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown Role: " + name);
    }
}
