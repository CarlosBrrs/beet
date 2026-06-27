package com.beet.backend.modules.cash.domain.usecase;

import com.beet.backend.modules.cash.domain.api.CashSessionQueryPort;
import com.beet.backend.modules.cash.domain.api.CashSessionServicePort;
import com.beet.backend.modules.cash.domain.api.BusinessDayQueryPort;
import com.beet.backend.modules.cash.domain.api.CashOperationsServicePort;
import com.beet.backend.modules.cash.domain.exception.CashRegisterNotFoundException;
import com.beet.backend.modules.cash.domain.exception.CashSessionConflictException;
import com.beet.backend.modules.cash.domain.exception.CashSessionNotFoundException;
import com.beet.backend.modules.cash.domain.model.CashRegisterDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.cash.domain.model.CashSessionSummary;
import com.beet.backend.modules.cash.domain.spi.CashRegisterPersistencePort;
import com.beet.backend.modules.cash.domain.spi.CashSessionPersistencePort;
import com.beet.backend.shared.domain.exception.AccessDeniedException;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashSessionUseCase implements CashSessionServicePort, CashSessionQueryPort {

    private final CashRegisterPersistencePort registerPersistencePort;
    private final CashSessionPersistencePort sessionPersistencePort;
    private final BusinessDayQueryPort businessDayQuery;
    private final CashOperationsServicePort cashOperationsService;

    @Override
    @Transactional
    public CashSessionDomain openSession(UUID restaurantId, UUID cashRegisterId, UUID userId,
            UUID deviceId, BigDecimal openingAmount, String notes) {
        CashRegisterDomain register = loadRegister(restaurantId, cashRegisterId);
        UUID businessDayId = businessDayQuery.requireOpenBusinessDay(restaurantId).getId();
        if (!Boolean.TRUE.equals(register.getIsActive())) {
            throw new AccessDeniedException("Cash register is inactive");
        }
        if (register.getDeviceId() != null && !register.getDeviceId().equals(deviceId)) {
            throw new AccessDeniedException("Cash register is assigned to another device");
        }
        if (sessionPersistencePort.findOpenByRegisterId(cashRegisterId).isPresent()) {
            throw CashSessionConflictException.openRegister();
        }
        if (sessionPersistencePort.findOpenByRestaurantAndDevice(restaurantId, deviceId).isPresent()) {
            throw CashSessionConflictException.openDevice();
        }
        if (register.getDeviceId() == null) {
            register.setDeviceId(deviceId);
            register.setUpdatedBy(userId);
            registerPersistencePort.update(register);
        }

        return sessionPersistencePort.create(CashSessionDomain.builder()
                .restaurantId(restaurantId)
                .businessDayId(businessDayId)
                .cashRegisterId(cashRegisterId)
                .status(CashSessionStatus.OPEN)
                .openedBy(userId)
                .openedDeviceId(deviceId)
                .openingAmount(requireNonNegative(openingAmount, "openingAmount"))
                .notes(notes)
                .build());
    }

    @Override
    @Transactional
    public CashSessionDomain closeSession(UUID restaurantId, UUID sessionId, UUID userId,
            UUID deviceId, BigDecimal countedCash, String differenceReason, String notes) {
        cashOperationsService.closeSession(
                restaurantId, sessionId, userId, deviceId, countedCash, differenceReason, notes, false);
        return sessionPersistencePort.findSessionById(sessionId)
                .orElseThrow(() -> CashSessionNotFoundException.forId(sessionId));
    }

    @Override
    @Transactional
    public CashSessionDomain forceCloseSession(UUID restaurantId, UUID sessionId, UUID userId,
            UUID deviceId, BigDecimal countedCash, String differenceReason, String notes) {
        cashOperationsService.closeSession(
                restaurantId, sessionId, userId, deviceId, countedCash, differenceReason, notes, true);
        return sessionPersistencePort.findSessionById(sessionId)
                .orElseThrow(() -> CashSessionNotFoundException.forId(sessionId));
    }

    CashSessionDomain closeSession(UUID restaurantId, UUID sessionId, UUID userId,
            UUID deviceId, BigDecimal closingAmount, String notes) {
        CashSessionDomain session = loadOpenSession(restaurantId, sessionId);
        if (!session.getOpenedDeviceId().equals(deviceId)) {
            throw new AccessDeniedException("Cash session belongs to another device");
        }
        if (!session.getOpenedBy().equals(userId)) {
            throw new AccessDeniedException("Cash session belongs to another user");
        }
        return legacyCloseAndRelease(session, userId, deviceId, closingAmount, notes);
    }

    CashSessionDomain forceCloseSession(UUID restaurantId, UUID sessionId, UUID userId,
            UUID deviceId, BigDecimal closingAmount, String notes) {
        return legacyCloseAndRelease(
                loadOpenSession(restaurantId, sessionId), userId, deviceId, closingAmount, notes);
    }

    @Override
    public CashSessionDomain getActiveSession(UUID restaurantId, UUID deviceId) {
        return sessionPersistencePort.findOpenByRestaurantAndDevice(restaurantId, deviceId)
                .orElseThrow(() -> CashSessionNotFoundException.forDevice(deviceId));
    }

    @Override
    @Transactional
    public CashSessionDomain getSessionForUpdate(UUID restaurantId, UUID sessionId) {
        CashSessionDomain session = sessionPersistencePort.findSessionByIdForUpdate(sessionId)
                .orElseThrow(() -> CashSessionNotFoundException.forId(sessionId));
        if (!restaurantId.equals(session.getRestaurantId())) {
            throw new AccessDeniedException("Cash session does not belong to restaurant");
        }
        return session;
    }

    @Override
    public PageResponse<CashSessionSummary> listSessions(
            List<UUID> restaurantIds,
            CashSessionStatus status,
            LocalDate from,
            LocalDate to,
            String timeZone,
            UUID cashRegisterId,
            int page,
            int size) {
        if (restaurantIds.isEmpty()) {
            return PageResponse.of(List.of(), 0, page, size);
        }
        return sessionPersistencePort.findSessionsPaged(
                restaurantIds, status, from, to, timeZone, cashRegisterId, page, size);
    }

    private CashRegisterDomain loadRegister(UUID restaurantId, UUID cashRegisterId) {
        CashRegisterDomain register = registerPersistencePort.findRegisterByIdForUpdate(cashRegisterId)
                .orElseThrow(() -> CashRegisterNotFoundException.forId(cashRegisterId));
        if (!register.getRestaurantId().equals(restaurantId)) {
            throw new AccessDeniedException("Cash register does not belong to restaurant");
        }
        return register;
    }

    private CashSessionDomain loadOpenSession(UUID restaurantId, UUID sessionId) {
        CashSessionDomain session = sessionPersistencePort.findSessionById(sessionId)
                .orElseThrow(() -> CashSessionNotFoundException.forId(sessionId));
        if (!session.getRestaurantId().equals(restaurantId)) {
            throw new AccessDeniedException("Cash session does not belong to restaurant");
        }
        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw CashSessionConflictException.alreadyClosed();
        }
        return session;
    }

    private CashSessionDomain legacyCloseAndRelease(CashSessionDomain session, UUID userId,
            UUID deviceId, BigDecimal closingAmount, String notes) {
        CashSessionDomain closed = sessionPersistencePort.close(
                session.getId(), userId, deviceId,
                requireNonNegative(closingAmount, "closingAmount"), notes);
        CashRegisterDomain register = loadRegister(session.getRestaurantId(), session.getCashRegisterId());
        register.setDeviceId(null);
        register.setUpdatedBy(userId);
        registerPersistencePort.update(register);
        return closed;
    }

    private BigDecimal requireNonNegative(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to zero");
        }
        return amount;
    }
}
