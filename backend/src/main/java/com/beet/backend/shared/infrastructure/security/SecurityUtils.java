package com.beet.backend.shared.infrastructure.security;

import com.beet.backend.modules.user.domain.exception.UserNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public static UUID getAuthenticatedUserId() {
        return getAuthenticatedUserDetails().getId();
    }

    /**
     * Returns the effective owner ID for multi-tenant queries.
     * - For owners: returns their own userId (ownerId is null).
     * - For employees: returns the ownerId of the owner who created them.
     */
    public static UUID getEffectiveOwnerId() {
        CustomUserDetails details = getAuthenticatedUserDetails();
        return details.getOwnerId() != null ? details.getOwnerId() : details.getId();
    }

    public static CustomUserDetails getAuthenticatedUserDetails() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        throw UserNotFoundException.forUsername(
                authentication != null ? authentication.getName() : "unknown");
    }
}
