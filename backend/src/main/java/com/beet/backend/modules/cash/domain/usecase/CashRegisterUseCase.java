package com.beet.backend.modules.cash.domain.usecase;

import com.beet.backend.modules.cash.domain.api.CashRegisterServicePort;
import com.beet.backend.modules.cash.domain.exception.CashRegisterAlreadyExistsException;
import com.beet.backend.modules.cash.domain.exception.CashRegisterNotFoundException;
import com.beet.backend.modules.cash.domain.exception.CashSessionConflictException;
import com.beet.backend.modules.cash.domain.model.CashRegisterDomain;
import com.beet.backend.modules.cash.domain.spi.CashRegisterPersistencePort;
import com.beet.backend.modules.cash.domain.spi.CashSessionPersistencePort;
import com.beet.backend.shared.domain.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashRegisterUseCase implements CashRegisterServicePort {

    private final CashRegisterPersistencePort persistencePort;
    private final CashSessionPersistencePort sessionPersistencePort;

    @Override
    @Transactional
    public CashRegisterDomain create(CashRegisterDomain register) {
        if (persistencePort.existsByName(register.getRestaurantId(), register.getName())) {
            throw CashRegisterAlreadyExistsException.forName(register.getName());
        }
        if (register.getDeviceId() != null
                && persistencePort.existsByDevice(register.getRestaurantId(), register.getDeviceId())) {
            throw CashRegisterAlreadyExistsException.forDevice(register.getDeviceId().toString());
        }
        if (register.getIsActive() == null) {
            register.setIsActive(true);
        }
        return persistencePort.save(register);
    }

    @Override
    @Transactional
    public CashRegisterDomain update(CashRegisterDomain register) {
        CashRegisterDomain existing = persistencePort.findRegisterById(register.getId())
                .orElseThrow(() -> CashRegisterNotFoundException.forId(register.getId()));

        if (!existing.getRestaurantId().equals(register.getRestaurantId())) {
            throw new AccessDeniedException("Cash register does not belong to restaurant");
        }

        String updatedName = register.getName() != null ? register.getName() : existing.getName();
        if (!existing.getName().equalsIgnoreCase(updatedName)
                && persistencePort.existsByName(register.getRestaurantId(), updatedName)) {
            throw CashRegisterAlreadyExistsException.forName(updatedName);
        }

        if (register.getDeviceId() != null
                && (existing.getDeviceId() == null || !existing.getDeviceId().equals(register.getDeviceId()))
                && persistencePort.existsByDevice(register.getRestaurantId(), register.getDeviceId())) {
            throw CashRegisterAlreadyExistsException.forDevice(register.getDeviceId().toString());
        }

        existing.setName(updatedName);
        if (register.getDeviceId() != null) {
            existing.setDeviceId(register.getDeviceId());
        }
        if (register.getIsActive() != null) {
            if (!register.getIsActive() && Boolean.TRUE.equals(existing.getIsActive())) {
                ensureNoOpenSession(existing.getId());
            }
            existing.setIsActive(register.getIsActive());
        }
        if (register.getNotes() != null) {
            existing.setNotes(register.getNotes());
        }
        existing.setUpdatedBy(register.getUpdatedBy());

        return persistencePort.update(existing);
    }

    @Override
    @Transactional
    public CashRegisterDomain deactivate(UUID restaurantId, UUID registerId, UUID updatedBy) {
        CashRegisterDomain existing = persistencePort.findRegisterById(registerId)
                .orElseThrow(() -> CashRegisterNotFoundException.forId(registerId));
        if (!existing.getRestaurantId().equals(restaurantId)) {
            throw new AccessDeniedException("Cash register does not belong to restaurant");
        }
        ensureNoOpenSession(registerId);
        existing.setIsActive(false);
        existing.setUpdatedBy(updatedBy);
        return persistencePort.update(existing);
    }

    @Override
    public CashRegisterDomain getById(UUID registerId) {
        return persistencePort.findRegisterById(registerId)
                .orElseThrow(() -> CashRegisterNotFoundException.forId(registerId));
    }

    @Override
    public List<CashRegisterDomain> listByRestaurant(UUID restaurantId) {
        return persistencePort.findByRestaurantId(restaurantId);
    }

    private void ensureNoOpenSession(UUID registerId) {
        if (sessionPersistencePort.findOpenByRegisterId(registerId).isPresent()) {
            throw CashSessionConflictException.openRegister();
        }
    }
}
