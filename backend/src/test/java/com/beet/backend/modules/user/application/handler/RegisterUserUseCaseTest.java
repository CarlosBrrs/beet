package com.beet.backend.modules.user.application.handler;

import com.beet.backend.modules.user.domain.exception.UserAlreadyExistsException;
import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.domain.spi.UserPersistencePort;
import com.beet.backend.modules.subscription.domain.exception.SubscriptionPlanNotFoundException;
import com.beet.backend.modules.user.domain.usecase.RegisterUserUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserPersistencePort userPersistencePort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegisterUserUseCase useCase;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        UUID planId = UUID.randomUUID();
        User inputUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .firstLastname("Doe")
                .phoneNumber("1234567890")
                .username("johndoe")
                .subscriptionPlanId(planId)
                .build();

        when(userPersistencePort.existsByEmail(any())).thenReturn(false);
        when(userPersistencePort.existsByUsername(any())).thenReturn(false);
        when(userPersistencePort.existsByPhoneNumber(any())).thenReturn(false);
        when(userPersistencePort.existsSubscriptionPlan(planId)).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");

        // Mock save to return a user with an ID simulating DB generation
        when(userPersistencePort.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            // Simulate DB assigning ID
            return User.builder()
                    .id(UUID.randomUUID()) // DB generated ID
                    .email(u.getEmail())
                    .passwordHash(u.getPasswordHash())
                    .firstName(u.getFirstName())
                    .firstLastname(u.getFirstLastname())
                    .phoneNumber(u.getPhoneNumber())
                    .username(u.getUsername())
                    .subscriptionPlanId(u.getSubscriptionPlanId())
                    .ownerId(u.getOwnerId())
                    .build();
        });

        // Act
        User result = useCase.register(inputUser);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId()); // ID should be present in result
        assertEquals("hashed_password", result.getPasswordHash());
        assertNull(result.getOwnerId()); // Should be null for Owner
        assertEquals("John Doe", result.getFullName());

        // Verify that save was called with a user that has NO ID yet
        verify(userPersistencePort).save(argThat(u -> u.getId() == null));
    }

    @Test
    void shouldThrowException_WhenEmailExists() {
        // Arrange
        User inputUser = User.builder().email("test@example.com").build();
        when(userPersistencePort.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> useCase.register(inputUser));
        verify(userPersistencePort, never()).save(any());
    }

    @Test
    void shouldThrowException_WhenPlanNotFound() {
        // Arrange
        UUID planId = UUID.randomUUID();
        User inputUser = User.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .firstLastname("Doe")
                .phoneNumber("1234567890")
                .username("johndoe")
                .subscriptionPlanId(planId)
                .build();

        when(userPersistencePort.existsByEmail(any())).thenReturn(false);
        when(userPersistencePort.existsSubscriptionPlan(planId)).thenReturn(false);

        // Act & Assert
        assertThrows(SubscriptionPlanNotFoundException.class, () -> useCase.register(inputUser));
    }
}
