package com.beet.backend.shared.infrastructure.security;

import com.beet.backend.modules.role.application.dto.UserPermissionEntry;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.modules.role.domain.spi.RolePersistencePort;
import com.beet.backend.shared.domain.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.UUID;

/**
 * AOP interceptor that enforces @RequiresPermission annotations on controller
 * methods.
 *
 * Extracts restaurantId from the method's @PathVariable("restaurantId")
 * parameter,
 * then checks the user's permissions for that restaurant against the required
 * module + action.
 *
 * OWNER role (ALL:ALL) bypasses all permission checks.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionInterceptor {

    private final RolePersistencePort rolePersistencePort;

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission)
            throws Throwable {
        PermissionModule requiredModule = requiresPermission.module();
        PermissionAction requiredAction = requiresPermission.action();

        UUID userId = SecurityUtils.getAuthenticatedUserId();
        UUID restaurantId = extractRestaurantId(joinPoint);

        // TODO: Cache permission lookups per (userId, restaurantId) to avoid repeated
        // DB queries within a session
        List<UserPermissionEntry> permissions = rolePersistencePort.findAllPermissionsForUser(userId);

        boolean authorized = permissions.stream().anyMatch(entry -> {
            // Check global OWNER entry (restaurantId = null, ALL:ALL)
            if (entry.restaurantId() == null) {
                List<PermissionAction> allActions = entry.permissions().get(PermissionModule.ALL);
                if (allActions != null && allActions.contains(PermissionAction.ALL)) {
                    return true; // OWNER bypasses all checks
                }
            }

            // Check restaurant-specific entry
            if (restaurantId.equals(entry.restaurantId())) {
                // Check exact module + action
                List<PermissionAction> moduleActions = entry.permissions().get(requiredModule);
                if (moduleActions != null) {
                    return moduleActions.contains(requiredAction) || moduleActions.contains(PermissionAction.ALL);
                }
            }

            return false;
        });

        if (!authorized) {
            throw AccessDeniedException.forPermission(
                    requiredModule.name(), requiredAction.name(), restaurantId.toString());
        }

        return joinPoint.proceed();
    }

    /**
     * Extracts the restaurantId from the method's @PathVariable parameters.
     * Looks for a parameter annotated with @PathVariable whose name matches
     * "restaurantId"
     * or whose @PathVariable value/name is "restaurantId".
     */
    private UUID extractRestaurantId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String paramName = pathVariable.value().isEmpty() ? pathVariable.name() : pathVariable.value();
                if (paramName.isEmpty()) {
                    paramName = parameters[i].getName();
                }
                if ("restaurantId".equals(paramName) && args[i] instanceof UUID) {
                    return (UUID) args[i];
                }
            }
        }

        throw new IllegalStateException(
                "@RequiresPermission used on method " + method.getName()
                        + " but no @PathVariable UUID restaurantId found");
    }
}
