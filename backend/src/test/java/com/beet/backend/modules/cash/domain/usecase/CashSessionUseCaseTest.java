package com.beet.backend.modules.cash.domain.usecase;

import com.beet.backend.modules.cash.domain.model.CashRegisterDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.cash.domain.spi.CashRegisterPersistencePort;
import com.beet.backend.modules.cash.domain.spi.CashSessionPersistencePort;
import com.beet.backend.shared.domain.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashSessionUseCaseTest {

    @Mock
    private CashRegisterPersistencePort registerPersistencePort;

    @Mock
    private CashSessionPersistencePort sessionPersistencePort;

    @InjectMocks
    private CashSessionUseCase useCase;

    @Test
    void shouldBindRegisterToDeviceWhenOpeningSession() {
        UUID restaurantId = UUID.randomUUID();
        UUID registerId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        CashRegisterDomain register = CashRegisterDomain.builder()
                .id(registerId)
                .restaurantId(restaurantId)
                .isActive(true)
                .build();

        when(registerPersistencePort.findRegisterByIdForUpdate(registerId)).thenReturn(Optional.of(register));
        when(sessionPersistencePort.findOpenByRegisterId(registerId)).thenReturn(Optional.empty());
        when(sessionPersistencePort.findOpenByRestaurantAndDevice(restaurantId, deviceId))
                .thenReturn(Optional.empty());
        when(sessionPersistencePort.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CashSessionDomain opened = useCase.openSession(
                restaurantId, registerId, userId, deviceId, BigDecimal.ZERO, null);

        assertEquals(deviceId, register.getDeviceId());
        assertEquals(userId, opened.getOpenedBy());
        assertEquals(deviceId, opened.getOpenedDeviceId());
        verify(registerPersistencePort).update(register);
    }

    @Test
    void shouldCloseAndReleaseRegisterForOpeningUserAndDevice() {
        UUID restaurantId = UUID.randomUUID();
        UUID registerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        CashSessionDomain openSession = openSession(sessionId, restaurantId, registerId, userId, deviceId);
        CashRegisterDomain register = boundRegister(registerId, restaurantId, deviceId);
        CashSessionDomain closedSession = openSession.toBuilder().status(CashSessionStatus.CLOSED).build();

        when(sessionPersistencePort.findSessionById(sessionId)).thenReturn(Optional.of(openSession));
        when(sessionPersistencePort.close(sessionId, userId, deviceId, BigDecimal.ZERO, null))
                .thenReturn(closedSession);
        when(registerPersistencePort.findRegisterByIdForUpdate(registerId)).thenReturn(Optional.of(register));
        when(registerPersistencePort.update(register)).thenReturn(register);

        CashSessionDomain result = useCase.closeSession(
                restaurantId, sessionId, userId, deviceId, BigDecimal.ZERO, null);

        assertEquals(closedSession, result);
        assertNull(register.getDeviceId());
        verify(registerPersistencePort).update(register);
    }

    @Test
    void shouldRejectNormalCloseFromAnotherDevice() {
        UUID restaurantId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CashSessionDomain session = openSession(
                sessionId, restaurantId, UUID.randomUUID(), userId, UUID.randomUUID());

        when(sessionPersistencePort.findSessionById(sessionId)).thenReturn(Optional.of(session));

        assertThrows(AccessDeniedException.class, () -> useCase.closeSession(
                restaurantId, sessionId, userId, UUID.randomUUID(), BigDecimal.ZERO, null));

        verify(sessionPersistencePort, never()).close(any(), any(), any(), any(), any());
        verify(registerPersistencePort, never()).update(any());
    }

    @Test
    void shouldRejectNormalCloseFromAnotherUser() {
        UUID restaurantId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        CashSessionDomain session = openSession(
                sessionId, restaurantId, UUID.randomUUID(), UUID.randomUUID(), deviceId);

        when(sessionPersistencePort.findSessionById(sessionId)).thenReturn(Optional.of(session));

        assertThrows(AccessDeniedException.class, () -> useCase.closeSession(
                restaurantId, sessionId, UUID.randomUUID(), deviceId, BigDecimal.ZERO, null));

        verify(sessionPersistencePort, never()).close(any(), any(), any(), any(), any());
        verify(registerPersistencePort, never()).update(any());
    }

    @Test
    void shouldForceCloseAndReleaseRegisterFromAnotherUserAndDevice() {
        UUID restaurantId = UUID.randomUUID();
        UUID registerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID openedDeviceId = UUID.randomUUID();
        UUID closingUserId = UUID.randomUUID();
        UUID closingDeviceId = UUID.randomUUID();
        CashSessionDomain openSession = openSession(
                sessionId, restaurantId, registerId, UUID.randomUUID(), openedDeviceId);
        CashRegisterDomain register = boundRegister(registerId, restaurantId, openedDeviceId);
        CashSessionDomain closedSession = openSession.toBuilder().status(CashSessionStatus.CLOSED).build();

        when(sessionPersistencePort.findSessionById(sessionId)).thenReturn(Optional.of(openSession));
        when(sessionPersistencePort.close(sessionId, closingUserId, closingDeviceId, BigDecimal.ZERO, null))
                .thenReturn(closedSession);
        when(registerPersistencePort.findRegisterByIdForUpdate(registerId)).thenReturn(Optional.of(register));
        when(registerPersistencePort.update(register)).thenReturn(register);

        CashSessionDomain result = useCase.forceCloseSession(
                restaurantId, sessionId, closingUserId, closingDeviceId, BigDecimal.ZERO, null);

        assertEquals(closedSession, result);
        assertNull(register.getDeviceId());
        verify(registerPersistencePort).update(register);
    }

    @Test
    void shouldRejectOpeningSessionForRegisterFromAnotherRestaurant() {
        UUID restaurantId = UUID.randomUUID();
        UUID registerId = UUID.randomUUID();
        CashRegisterDomain foreignRegister = CashRegisterDomain.builder()
                .id(registerId)
                .restaurantId(UUID.randomUUID())
                .isActive(true)
                .build();

        when(registerPersistencePort.findRegisterByIdForUpdate(registerId)).thenReturn(Optional.of(foreignRegister));

        assertThrows(AccessDeniedException.class, () -> useCase.openSession(
                restaurantId,
                registerId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ZERO,
                null));

        verify(sessionPersistencePort, never()).create(any());
    }

    @Test
    void shouldRejectClosingSessionFromAnotherRestaurant() {
        UUID restaurantId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        CashSessionDomain foreignSession = CashSessionDomain.builder()
                .id(sessionId)
                .restaurantId(UUID.randomUUID())
                .status(CashSessionStatus.OPEN)
                .openedDeviceId(UUID.randomUUID())
                .build();

        when(sessionPersistencePort.findSessionById(sessionId)).thenReturn(Optional.of(foreignSession));

        assertThrows(AccessDeniedException.class, () -> useCase.closeSession(
                restaurantId,
                sessionId,
                UUID.randomUUID(),
                foreignSession.getOpenedDeviceId(),
                BigDecimal.ZERO,
                null));

        verify(sessionPersistencePort, never()).close(any(), any(), any(), any(), any());
    }

    private CashSessionDomain openSession(
            UUID sessionId, UUID restaurantId, UUID registerId, UUID userId, UUID deviceId) {
        return CashSessionDomain.builder()
                .id(sessionId)
                .restaurantId(restaurantId)
                .cashRegisterId(registerId)
                .status(CashSessionStatus.OPEN)
                .openedBy(userId)
                .openedDeviceId(deviceId)
                .build();
    }

    private CashRegisterDomain boundRegister(UUID registerId, UUID restaurantId, UUID deviceId) {
        return CashRegisterDomain.builder()
                .id(registerId)
                .restaurantId(restaurantId)
                .deviceId(deviceId)
                .isActive(true)
                .build();
    }
}
