package com.beet.backend.modules.cash.domain.usecase;

import com.beet.backend.modules.cash.domain.api.BusinessDayQueryPort;
import com.beet.backend.modules.cash.domain.api.CashOperationsServicePort;
import com.beet.backend.modules.cash.domain.exception.CashRegisterNotFoundException;
import com.beet.backend.modules.cash.domain.exception.CashSessionConflictException;
import com.beet.backend.modules.cash.domain.exception.CashSessionNotFoundException;
import com.beet.backend.modules.cash.domain.model.BusinessDayClosureDomain;
import com.beet.backend.modules.cash.domain.model.BusinessDayEventType;
import com.beet.backend.modules.cash.domain.model.BusinessDayStatus;
import com.beet.backend.modules.cash.domain.model.CashMovementDomain;
import com.beet.backend.modules.cash.domain.model.CashMovementReason;
import com.beet.backend.modules.cash.domain.model.CashMovementStatus;
import com.beet.backend.modules.cash.domain.model.CashRegisterDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionReconciliationDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.cash.domain.model.PaymentTotalSnapshot;
import com.beet.backend.modules.cash.domain.model.RestaurantBusinessDayDomain;
import com.beet.backend.modules.cash.domain.spi.CashOperationsPersistencePort;
import com.beet.backend.modules.cash.domain.spi.CashRegisterPersistencePort;
import com.beet.backend.modules.cash.domain.spi.CashSessionPersistencePort;
import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.model.RestaurantSettings;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPersistencePort;
import com.beet.backend.shared.domain.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashOperationsUseCase implements CashOperationsServicePort, BusinessDayQueryPort {
    private static final String DEFAULT_TIME_ZONE = "America/Bogota";

    private final CashOperationsPersistencePort operationsPersistence;
    private final CashSessionPersistencePort sessionPersistence;
    private final CashRegisterPersistencePort registerPersistence;
    private final RestaurantPersistencePort restaurantPersistence;

    @Override
    public Optional<RestaurantBusinessDayDomain> findOpenBusinessDay(UUID restaurantId) {
        return operationsPersistence.findOpenBusinessDay(restaurantId);
    }

    @Override
    public RestaurantBusinessDayDomain requireOpenBusinessDay(UUID restaurantId) {
        return findOpenBusinessDay(restaurantId)
                .orElseThrow(() -> new IllegalStateException(
                        "A business day must be open before performing commercial operations."));
    }

    @Override
    public RestaurantBusinessDayDomain getCurrentBusinessDay(UUID restaurantId) {
        return findOpenBusinessDay(restaurantId)
                .map(day -> operationsPersistence.loadBusinessDayBlockers(restaurantId, day.getId()))
                .orElse(null);
    }

    @Override
    public List<RestaurantBusinessDayDomain> listBusinessDays(UUID restaurantId) {
        return operationsPersistence.findBusinessDays(restaurantId);
    }

    @Override
    public RestaurantBusinessDayDomain getBusinessDay(UUID restaurantId, UUID businessDayId) {
        return operationsPersistence.loadBusinessDayBlockers(restaurantId, businessDayId);
    }

    @Override
    @Transactional
    public RestaurantBusinessDayDomain openBusinessDay(UUID restaurantId, UUID userId) {
        if (findOpenBusinessDay(restaurantId).isPresent()) {
            throw new IllegalArgumentException("Restaurant already has an open business day.");
        }
        RestaurantDomain restaurant = loadRestaurant(restaurantId);
        String timeZone = resolveTimeZone(restaurant);
        LocalDate businessDate = LocalDate.now(ZoneId.of(timeZone));
        if (operationsPersistence.findBusinessDayByDate(restaurantId, businessDate).isPresent()) {
            throw new IllegalArgumentException(
                    "The current business date already exists. Reopen it instead.");
        }
        RestaurantBusinessDayDomain created = operationsPersistence.createBusinessDay(
                RestaurantBusinessDayDomain.builder()
                        .restaurantId(restaurantId)
                        .businessDate(businessDate)
                        .timeZoneSnapshot(timeZone)
                        .status(BusinessDayStatus.OPEN)
                        .openedBy(userId)
                        .build());
        operationsPersistence.saveBusinessDayEvent(
                restaurantId, created.getId(), BusinessDayEventType.OPENED, userId, null);
        return created;
    }

    @Override
    @Transactional
    public BusinessDayClosureDomain closeBusinessDay(
            UUID restaurantId, UUID businessDayId, UUID userId, String notes) {
        RestaurantBusinessDayDomain day = operationsPersistence.loadBusinessDayBlockers(
                restaurantId, businessDayId);
        if (day.getStatus() != BusinessDayStatus.OPEN) {
            throw new IllegalArgumentException("Business day is not open.");
        }
        if (day.getOpenSessionCount() > 0
                || day.getPendingOrderCount() > 0
                || day.getMissingReconciliationCount() > 0
                || day.getUnexplainedDifferenceCount() > 0) {
            throw new IllegalArgumentException(
                    "Business day cannot close while sessions, orders, refunds or reconciliations remain pending.");
        }
        List<PaymentTotalSnapshot> totals =
                operationsPersistence.calculateBusinessDayPaymentTotals(businessDayId);
        BigDecimal payments = sum(totals, TotalField.PAYMENT);
        BigDecimal tips = sum(totals, TotalField.TIP);
        BigDecimal refunds = sum(totals, TotalField.REFUND);
        BigDecimal cashIn = operationsPersistence.sumBusinessDayMovements(businessDayId, "IN");
        BigDecimal cashOut = operationsPersistence.sumBusinessDayMovements(businessDayId, "OUT");
        BigDecimal expected = operationsPersistence.sumBusinessDayExpectedCash(businessDayId);
        BigDecimal counted = operationsPersistence.sumBusinessDayCountedCash(businessDayId);
        BusinessDayClosureDomain closure = operationsPersistence.createBusinessDayClosure(
                BusinessDayClosureDomain.builder()
                        .restaurantId(restaurantId)
                        .businessDayId(businessDayId)
                        .paymentsTotal(payments)
                        .tipsTotal(tips)
                        .refundsTotal(refunds)
                        .cashInTotal(cashIn)
                        .cashOutTotal(cashOut)
                        .expectedCashTotal(expected)
                        .countedCashTotal(counted)
                        .differenceTotal(counted.subtract(expected))
                        .notes(notes)
                        .closedBy(userId)
                        .paymentTotals(totals)
                        .build());
        operationsPersistence.closeBusinessDay(businessDayId, userId);
        operationsPersistence.saveBusinessDayEvent(
                restaurantId, businessDayId, BusinessDayEventType.CLOSED, userId, notes);
        return closure;
    }

    @Override
    @Transactional
    public RestaurantBusinessDayDomain reopenBusinessDay(
            UUID restaurantId, UUID businessDayId, UUID userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reopen reason is required.");
        }
        if (findOpenBusinessDay(restaurantId).isPresent()) {
            throw new IllegalArgumentException("Another business day is already open.");
        }
        RestaurantBusinessDayDomain day = loadBusinessDay(restaurantId, businessDayId);
        LocalDate today = LocalDate.now(ZoneId.of(resolveTimeZone(loadRestaurant(restaurantId))));
        if (!today.equals(day.getBusinessDate())) {
            throw new IllegalArgumentException("Only the current local business date can be reopened.");
        }
        RestaurantBusinessDayDomain reopened = operationsPersistence.reopenBusinessDay(businessDayId, userId);
        operationsPersistence.saveBusinessDayEvent(
                restaurantId, businessDayId, BusinessDayEventType.REOPENED, userId, reason.trim());
        return reopened;
    }

    @Override
    public List<CashMovementDomain> listMovements(UUID restaurantId, UUID sessionId) {
        loadSession(restaurantId, sessionId);
        return operationsPersistence.findMovements(restaurantId, sessionId);
    }

    @Override
    @Transactional
    public CashMovementDomain recordMovement(CashMovementDomain movement, UUID userId, UUID deviceId) {
        operationsPersistence.lockSession(movement.getRestaurantId(), movement.getCashSessionId());
        CashSessionDomain session = loadOpenSession(movement.getRestaurantId(), movement.getCashSessionId());
        requireOpenBusinessDay(movement.getRestaurantId());
        if (!session.getOpenedDeviceId().equals(deviceId)) {
            throw new AccessDeniedException("Cash session belongs to another device.");
        }
        if (movement.getAmount() == null || movement.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Movement amount must be greater than zero.");
        }
        if ((movement.getReason() == CashMovementReason.OTHER
                || movement.getReason() == CashMovementReason.CORRECTION)
                && (movement.getNotes() == null || movement.getNotes().isBlank())) {
            throw new IllegalArgumentException("A note is required for this movement reason.");
        }
        movement.setBusinessDayId(session.getBusinessDayId());
        movement.setStatus(CashMovementStatus.RECORDED);
        movement.setCreatedBy(userId);
        movement.setCreatedDeviceId(deviceId);
        return operationsPersistence.saveMovement(movement);
    }

    @Override
    @Transactional
    public CashMovementDomain voidMovement(UUID restaurantId, UUID sessionId, UUID movementId,
            UUID userId, UUID deviceId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Void reason is required.");
        }
        operationsPersistence.lockSession(restaurantId, sessionId);
        CashSessionDomain session = loadOpenSession(restaurantId, sessionId);
        if (!session.getOpenedDeviceId().equals(deviceId)) {
            throw new AccessDeniedException("Cash session belongs to another device.");
        }
        if (operationsPersistence.findSessionClosure(restaurantId, sessionId).isPresent()) {
            throw new IllegalArgumentException("Movements cannot be voided after reconciliation.");
        }
        operationsPersistence.findMovement(restaurantId, sessionId, movementId)
                .orElseThrow(() -> new IllegalArgumentException("Cash movement not found."));
        return operationsPersistence.voidMovement(movementId, userId, deviceId, reason.trim());
    }

    @Override
    public CashSessionReconciliationDomain getReconciliation(UUID restaurantId, UUID sessionId) {
        CashSessionReconciliationDomain reconciliation =
                operationsPersistence.findSessionClosure(restaurantId, sessionId)
                        .orElseGet(() -> operationsPersistence.calculateSessionReconciliation(
                                restaurantId, sessionId));
        reconciliation.setBlind(!reconciliation.isClosed()
                && cashCountMode(restaurantId) == RestaurantSettings.CashCountMode.BLIND);
        if (reconciliation.isBlind()) {
            reconciliation.setExpectedCash(null);
            reconciliation.setPaymentsTotal(null);
            reconciliation.setTipsTotal(null);
            reconciliation.setRefundsTotal(null);
            reconciliation.setCashInTotal(null);
            reconciliation.setCashOutTotal(null);
            reconciliation.setPaymentTotals(List.of());
        }
        return reconciliation;
    }

    @Override
    @Transactional
    public CashSessionReconciliationDomain closeSession(UUID restaurantId, UUID sessionId,
            UUID userId, UUID deviceId, BigDecimal countedCash, String differenceReason,
            String notes, boolean force) {
        operationsPersistence.lockSession(restaurantId, sessionId);
        CashSessionDomain session = loadOpenSession(restaurantId, sessionId);
        if (!force) {
            if (!session.getOpenedDeviceId().equals(deviceId)) {
                throw new AccessDeniedException("Cash session belongs to another device.");
            }
            if (!session.getOpenedBy().equals(userId)) {
                throw new AccessDeniedException("Cash session belongs to another user.");
            }
        }
        if (countedCash == null || countedCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Counted cash must be greater than or equal to zero.");
        }
        CashSessionReconciliationDomain reconciliation =
                operationsPersistence.calculateSessionReconciliation(restaurantId, sessionId);
        BigDecimal difference = countedCash.subtract(reconciliation.getExpectedCash());
        if (difference.compareTo(BigDecimal.ZERO) != 0
                && (differenceReason == null || differenceReason.isBlank())) {
            throw new IllegalArgumentException("Difference reason is required when cash does not balance.");
        }
        reconciliation.setCountedCash(countedCash);
        reconciliation.setDifferenceAmount(difference);
        reconciliation.setDifferenceReason(differenceReason);
        reconciliation.setNotes(notes);
        reconciliation.setClosedBy(userId);
        reconciliation.setClosedDeviceId(deviceId);
        CashSessionReconciliationDomain saved =
                operationsPersistence.saveSessionClosure(reconciliation);
        sessionPersistence.close(sessionId, userId, deviceId, countedCash, notes);
        CashRegisterDomain register = registerPersistence.findRegisterByIdForUpdate(session.getCashRegisterId())
                .orElseThrow(() -> CashRegisterNotFoundException.forId(session.getCashRegisterId()));
        register.setDeviceId(null);
        register.setUpdatedBy(userId);
        registerPersistence.update(register);
        return saved;
    }

    private RestaurantBusinessDayDomain loadBusinessDay(UUID restaurantId, UUID businessDayId) {
        return operationsPersistence.findBusinessDay(restaurantId, businessDayId)
                .orElseThrow(() -> new IllegalArgumentException("Business day not found."));
    }

    private RestaurantDomain loadRestaurant(UUID restaurantId) {
        return restaurantPersistence.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found."));
    }

    private CashSessionDomain loadSession(UUID restaurantId, UUID sessionId) {
        CashSessionDomain session = sessionPersistence.findSessionById(sessionId)
                .orElseThrow(() -> CashSessionNotFoundException.forId(sessionId));
        if (!restaurantId.equals(session.getRestaurantId())) {
            throw new AccessDeniedException("Cash session does not belong to restaurant.");
        }
        return session;
    }

    private CashSessionDomain loadOpenSession(UUID restaurantId, UUID sessionId) {
        CashSessionDomain session = loadSession(restaurantId, sessionId);
        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw CashSessionConflictException.alreadyClosed();
        }
        return session;
    }

    private String resolveTimeZone(RestaurantDomain restaurant) {
        return restaurant.getSettings() == null
                || restaurant.getSettings().timeZone() == null
                || restaurant.getSettings().timeZone().isBlank()
                ? DEFAULT_TIME_ZONE
                : restaurant.getSettings().timeZone();
    }

    private RestaurantSettings.CashCountMode cashCountMode(UUID restaurantId) {
        RestaurantSettings settings = loadRestaurant(restaurantId).getSettings();
        return settings == null || settings.cashCountMode() == null
                ? RestaurantSettings.CashCountMode.BLIND
                : settings.cashCountMode();
    }

    private BigDecimal sum(List<PaymentTotalSnapshot> totals, TotalField field) {
        return totals.stream()
                .map(total -> switch (field) {
                    case PAYMENT -> total.paymentAmount();
                    case TIP -> total.tipAmount();
                    case REFUND -> total.refundAmount();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private enum TotalField {
        PAYMENT,
        TIP,
        REFUND
    }
}
