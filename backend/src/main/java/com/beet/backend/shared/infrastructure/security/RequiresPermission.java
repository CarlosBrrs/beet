package com.beet.backend.shared.infrastructure.security;

import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as requiring a specific permission on a restaurant.
 * The restaurantId is automatically extracted from the path variable named
 * "restaurantId".
 *
 * Usage:
 * {@code @RequiresPermission(module = PermissionModule.INVENTORY, action = PermissionAction.ACTIVATE)}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    PermissionModule module();

    PermissionAction action();
}
