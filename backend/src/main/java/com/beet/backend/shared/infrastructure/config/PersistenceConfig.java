package com.beet.backend.shared.infrastructure.config;

import com.beet.backend.shared.infrastructure.security.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
public class PersistenceConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication.getPrincipal().equals("anonymousUser")) {
                return Optional.empty();
            }

            if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                return Optional.of(userDetails.getId());
            }

            return Optional.empty();
        };
    }
}
