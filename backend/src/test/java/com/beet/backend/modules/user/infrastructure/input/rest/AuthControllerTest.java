package com.beet.backend.modules.user.infrastructure.input.rest;

import com.beet.backend.modules.user.application.dto.LoginRequest;
import com.beet.backend.modules.user.application.dto.LoginResponse;
import com.beet.backend.modules.user.application.dto.RegisterUserRequest;
import com.beet.backend.modules.user.application.dto.UserResponse;
import com.beet.backend.modules.user.application.handler.AuthHandler;
import com.beet.backend.modules.user.domain.exception.UserAlreadyExistsException;
import com.beet.backend.modules.subscription.domain.exception.SubscriptionPlanNotFoundException;
import com.beet.backend.shared.infrastructure.config.SecurityConfig;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Import SecurityConfig so we don't get 401s if default security is on
// We might need to mock JwtDecoder if using ResourceServer, but right now it's basic.
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @org.junit.jupiter.api.BeforeEach
        void setUp() throws Exception {
                org.mockito.Mockito.doAnswer(invocation -> {
                        jakarta.servlet.ServletRequest request = invocation.getArgument(0);
                        jakarta.servlet.ServletResponse response = invocation.getArgument(1);
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(request, response);
                        return null;
                }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        }

        @Test
        void shouldLoginSuccessfully() throws Exception {
                LoginRequest loginRequest = new LoginRequest("test@example.com", "password");
                UserResponse userResponse = UserResponse.builder()
                                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                                .email("test@example.com")
                                .build();
                LoginResponse loginResponse = new LoginResponse("jwt-token", userResponse);
                ApiGenericResponse<LoginResponse> apiResponse = ApiGenericResponse.success(loginResponse);

                when(authHandler.login(any(LoginRequest.class))).thenReturn(apiResponse);

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.token", is("jwt-token")))
                                .andExpect(jsonPath("$.data.user.email", is("test@example.com")));
        }

        @MockBean
        private AuthHandler authHandler;

        @MockBean
        private com.beet.backend.shared.infrastructure.security.JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockBean
        private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

        @MockBean
        private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @WithMockUser // Simulate a user (although endpoint is public, good practice)
        void shouldRegisterUser_WhenValidRequest() throws Exception {
                UUID planId = UUID.randomUUID();
                RegisterUserRequest request = new RegisterUserRequest(
                                "test@example.com", "password123", "John", null, "Doe", null, "1234567890", "johndoe",
                                planId);

                UserResponse userResponse = new UserResponse(UUID.randomUUID(), "test@example.com", "John Doe", "OWNER",
                                UUID.randomUUID());
                ApiGenericResponse<UserResponse> handlerResponse = ApiGenericResponse.success(userResponse);

                when(authHandler.register(any(RegisterUserRequest.class))).thenReturn(handlerResponse);

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.email").value("test@example.com"));
        }

        @Test
        @WithMockUser
        void shouldReturn400_WhenInvalidRequest() throws Exception {
                // Missing email, password too short
                RegisterUserRequest request = new RegisterUserRequest(
                                "", "pas", "John", null, "Doe", null, "1234567890", "johndoe", UUID.randomUUID());

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errorMessage").value(Matchers
                                                .containsString("MethodArgumentNotValidException")));
        }

        @Test
        @WithMockUser
        void shouldReturn409_WhenUserAlreadyExists() throws Exception {
                RegisterUserRequest request = new RegisterUserRequest(
                                "existing@example.com", "password123", "John", null, "Doe", null, "1234567890",
                                "johndoe", UUID.randomUUID());

                when(authHandler.register(any(RegisterUserRequest.class)))
                                .thenThrow(UserAlreadyExistsException
                                                .forEmail("existing@example.com"));

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errorMessage").value(
                                                "UserAlreadyExistsException: Email already in use: existing@example.com"));
        }

        @Test
        @WithMockUser
        void shouldReturn404_WhenPlanNotFound() throws Exception {
                UUID planId = UUID.randomUUID();
                RegisterUserRequest request = new RegisterUserRequest(
                                "new@example.com", "password123", "John", null, "Doe", null, "1234567890", "johndoe",
                                planId);

                when(authHandler.register(any(RegisterUserRequest.class)))
                                .thenThrow(SubscriptionPlanNotFoundException
                                                .forId(planId));

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errorMessage").value(
                                                "SubscriptionPlanNotFoundException: Subscription Plan not found: "
                                                                + planId));
        }

        // Note: We need a GlobalExceptionHandler to map UserAlreadyExistsException to
        // 409
        // Since we created Shared Exception classes but NO GlobalExceptionHandler yet,
        // the Controller would throw 500 by default for runtime exceptions.
        // I should probably add the GlobalExceptionHandler to Shared Kernel next.
}
