package com.beet.backend.modules.user.application.usecase;

import com.beet.backend.modules.user.application.dto.LoginRequest;
import com.beet.backend.modules.user.application.dto.LoginResponse;
import com.beet.backend.modules.user.application.dto.UserResponse;
import com.beet.backend.modules.user.application.mapper.UserServiceMapper;
import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.modules.user.domain.spi.UserPersistencePort;
import com.beet.backend.modules.user.domain.usecase.LoginUserUseCaseImpl;
import com.beet.backend.shared.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUserUseCaseImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserPersistencePort userPersistencePort;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserServiceMapper userServiceMapper;

    @InjectMocks
    private LoginUserUseCaseImpl loginUserUseCase;

    @Test
    void shouldLoginSuccessfully() {
        // Arrange
        String email = "test@example.com";
        String password = "password";
        String token = "jwt-token";

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hashedPwd")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(email)
                .build();

        when(userPersistencePort.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn(token);
        when(userServiceMapper.toResponse(user)).thenReturn(userResponse);

        // Act
        LoginResponse response = loginUserUseCase.login(email, password);

        // Assert
        assertNotNull(response);
        assertEquals(token, response.token());
        assertEquals(userResponse, response.user());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userPersistencePort).findByEmail(email);
        verify(jwtService).generateToken(any(UserDetails.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        String email = "nonexistent@example.com";

        when(userPersistencePort.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> loginUserUseCase.login(email, "password"));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
