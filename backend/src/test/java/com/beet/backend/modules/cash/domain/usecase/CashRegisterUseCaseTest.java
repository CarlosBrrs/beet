package com.beet.backend.modules.cash.domain.usecase;

import com.beet.backend.modules.cash.domain.exception.CashSessionConflictException;
import com.beet.backend.modules.cash.domain.model.CashRegisterDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.cash.domain.spi.CashRegisterPersistencePort;
import com.beet.backend.modules.cash.domain.spi.CashSessionPersistencePort;
import com.beet.backend.shared.domain.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashRegisterUseCaseTest {

    @Mock
    private CashRegisterPersistencePort registerPersistencePort;

    @Mock
    private CashSessionPersistencePort sessionPersistencePort;

    @InjectMocks
    private CashRegisterUseCase useCase;

    @Test
    void shouldReleaseDeviceBindingWhenRegisterHasNoOpenSession() {
        UUID restaurantId = UUID.randomUUID();
        UUID registerId = UUID.randomUUID();
        CashRegisterDomain register = boundRegister(registerId, restaurantId);

        when(registerPersistencePort.findRegisterByIdForUpdate(registerId)).thenReturn(Optional.of(register));
        when(sessionPersistencePort.findOpenByRegisterId(registerId)).thenReturn(Optional.empty());
        when(registerPersistencePort.update(register)).thenReturn(register);

        CashRegisterDomain released = useCase.releaseDeviceBinding(
                restaurantId, registerId, UUID.randomUUID());

        assertNull(released.getDeviceId());
        verify(registerPersistencePort).update(register);
    }

    @Test
    void shouldRejectReleaseDeviceBindingWhenRegisterHasOpenSession() {
        UUID restaurantId = UUID.randomUUID();
        UUID registerId = UUID.randomUUID();
        CashRegisterDomain register = boundRegister(registerId, restaurantId);

        when(registerPersistencePort.findRegisterByIdForUpdate(registerId)).thenReturn(Optional.of(register));
        when(sessionPersistencePort.findOpenByRegisterId(registerId))
                .thenReturn(Optional.of(CashSessionDomain.builder().build()));

        assertThrows(CashSessionConflictException.class, () -> useCase.releaseDeviceBinding(
                restaurantId, registerId, UUID.randomUUID()));

        verify(registerPersistencePort, never()).update(any());
    }

    @Test
    void shouldRejectReleaseDeviceBindingForAnotherRestaurant() {
        UUID registerId = UUID.randomUUID();
        CashRegisterDomain register = boundRegister(registerId, UUID.randomUUID());

        when(registerPersistencePort.findRegisterByIdForUpdate(registerId)).thenReturn(Optional.of(register));

        assertThrows(AccessDeniedException.class, () -> useCase.releaseDeviceBinding(
                UUID.randomUUID(), registerId, UUID.randomUUID()));

        verify(sessionPersistencePort, never()).findOpenByRegisterId(any());
        verify(registerPersistencePort, never()).update(any());
    }

    @Test
    void shouldTreatReleaseOfUnboundRegisterAsIdempotent() {
        UUID restaurantId = UUID.randomUUID();
        UUID registerId = UUID.randomUUID();
        CashRegisterDomain register = CashRegisterDomain.builder()
                .id(registerId)
                .restaurantId(restaurantId)
                .build();

        when(registerPersistencePort.findRegisterByIdForUpdate(registerId)).thenReturn(Optional.of(register));
        when(sessionPersistencePort.findOpenByRegisterId(registerId)).thenReturn(Optional.empty());

        CashRegisterDomain released = useCase.releaseDeviceBinding(
                restaurantId, registerId, UUID.randomUUID());

        assertSame(register, released);
        verify(registerPersistencePort, never()).update(any());
    }

    private CashRegisterDomain boundRegister(UUID registerId, UUID restaurantId) {
        return CashRegisterDomain.builder()
                .id(registerId)
                .restaurantId(restaurantId)
                .deviceId(UUID.randomUUID())
                .build();
    }
}
