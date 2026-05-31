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
    void shouldRejectOpeningSessionForRegisterFromAnotherRestaurant() {
        UUID restaurantId = UUID.randomUUID();
        UUID registerId = UUID.randomUUID();
        CashRegisterDomain foreignRegister = CashRegisterDomain.builder()
                .id(registerId)
                .restaurantId(UUID.randomUUID())
                .isActive(true)
                .build();

        when(registerPersistencePort.findRegisterById(registerId)).thenReturn(Optional.of(foreignRegister));

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
}
