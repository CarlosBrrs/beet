package com.beet.backend.modules.cash.application.handler;

import com.beet.backend.modules.cash.domain.api.CashRegisterServicePort;
import com.beet.backend.modules.cash.domain.api.CashSessionQueryPort;
import com.beet.backend.modules.cash.domain.api.CashSessionServicePort;
import com.beet.backend.modules.cash.domain.exception.CashSessionNotFoundException;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.restaurant.domain.api.RestaurantServicePort;
import com.beet.backend.modules.restaurant.domain.model.RestaurantWithRole;
import com.beet.backend.modules.role.application.dto.UserPermissionEntry;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.modules.role.domain.spi.RolePersistencePort;
import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.CustomUserDetails;
import com.beet.backend.shared.infrastructure.security.DeviceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class CashHandlerImplTest {

    @Mock
    private CashRegisterServicePort registerService;

    @Mock
    private CashSessionServicePort sessionService;

    @Mock
    private CashSessionQueryPort sessionQuery;

    @Mock
    private RestaurantServicePort restaurantService;

    @Mock
    private RolePersistencePort rolePersistence;

    @Mock
    private DeviceContext deviceContext;

    @InjectMocks
    private CashHandlerImpl handler;

    private UUID userId;

    @BeforeEach
    void setUpAuthentication() {
        userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("cash-manager@example.com")
                .passwordHash("hash")
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        lenient().when(sessionQuery.listSessions(anyList(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(PageResponse.of(List.of(), 0, 0, 20));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldListOnlyRestaurantsWithCashPermissionForScopedUser() {
        UUID allowedRestaurantId = UUID.randomUUID();
        UUID hiddenRestaurantId = UUID.randomUUID();
        when(rolePersistence.findAllPermissionsForUser(userId)).thenReturn(List.of(
                new UserPermissionEntry(
                        allowedRestaurantId,
                        "MANAGER",
                        Map.of(PermissionModule.CASH, List.of(PermissionAction.VIEW))),
                new UserPermissionEntry(
                        hiddenRestaurantId,
                        "COOK",
                        Map.of(PermissionModule.KITCHEN, List.of(PermissionAction.VIEW)))));

        handler.listAccountSessions(null, CashSessionStatus.OPEN, null, null, "America/Bogota", null, -1, 500);

        verify(sessionQuery).listSessions(
                eq(List.of(allowedRestaurantId)),
                eq(CashSessionStatus.OPEN),
                eq(null),
                eq(null),
                eq("America/Bogota"),
                eq(null),
                eq(0),
                eq(100));
        verify(restaurantService, never()).getRestaurantsWithRole(any());
    }

    @Test
    void shouldListAssignedRestaurantsForGlobalOwner() {
        UUID firstRestaurantId = UUID.randomUUID();
        UUID secondRestaurantId = UUID.randomUUID();
        when(rolePersistence.findAllPermissionsForUser(userId)).thenReturn(List.of(
                new UserPermissionEntry(
                        null,
                        "OWNER",
                        Map.of(PermissionModule.ALL, List.of(PermissionAction.ALL)))));
        when(restaurantService.getRestaurantsWithRole(userId)).thenReturn(List.of(
                restaurant(firstRestaurantId),
                restaurant(secondRestaurantId)));

        handler.listAccountSessions(null, CashSessionStatus.CLOSED, null, null, "America/Bogota", null, 0, 20);

        verify(sessionQuery).listSessions(
                eq(List.of(firstRestaurantId, secondRestaurantId)),
                eq(CashSessionStatus.CLOSED),
                eq(null),
                eq(null),
                eq("America/Bogota"),
                eq(null),
                eq(0),
                eq(20));
    }

    @Test
    void shouldReturnEmptyScopeWhenExplicitRestaurantIsNotAuthorized() {
        UUID allowedRestaurantId = UUID.randomUUID();
        when(rolePersistence.findAllPermissionsForUser(userId)).thenReturn(List.of(
                new UserPermissionEntry(
                        allowedRestaurantId,
                        "MANAGER",
                        Map.of(PermissionModule.CASH, List.of(PermissionAction.VIEW)))));

        handler.listAccountSessions(UUID.randomUUID(), CashSessionStatus.OPEN, null, null, "America/Bogota", null, 0, 20);

        verify(sessionQuery).listSessions(
                eq(List.of()),
                eq(CashSessionStatus.OPEN),
                eq(null),
                eq(null),
                eq("America/Bogota"),
                eq(null),
                eq(0),
                eq(20));
    }

    @Test
    void shouldReturnNullWhenCurrentDeviceHasNoActiveSession() {
        UUID restaurantId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        when(deviceContext.getDeviceId()).thenReturn(deviceId);
        when(sessionQuery.getActiveSession(restaurantId, deviceId))
                .thenThrow(CashSessionNotFoundException.forDevice(deviceId));

        assertNull(handler.getActiveSession(restaurantId).getData());
    }

    private RestaurantWithRole restaurant(UUID restaurantId) {
        return new RestaurantWithRole(
                restaurantId,
                "Restaurant",
                null,
                true,
                userId,
                null,
                "OWNER");
    }
}
