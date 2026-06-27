package com.beet.backend.modules.cash.domain.spi;

import com.beet.backend.modules.cash.domain.model.BusinessDayClosureDomain;
import com.beet.backend.modules.cash.domain.model.BusinessDayEventType;
import com.beet.backend.modules.cash.domain.model.CashMovementDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionReconciliationDomain;
import com.beet.backend.modules.cash.domain.model.PaymentTotalSnapshot;
import com.beet.backend.modules.cash.domain.model.RestaurantBusinessDayDomain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashOperationsPersistencePort {
    Optional<RestaurantBusinessDayDomain> findOpenBusinessDay(UUID restaurantId);

    Optional<RestaurantBusinessDayDomain> findBusinessDay(UUID restaurantId, UUID businessDayId);

    Optional<RestaurantBusinessDayDomain> findBusinessDayByDate(UUID restaurantId, LocalDate businessDate);

    List<RestaurantBusinessDayDomain> findBusinessDays(UUID restaurantId);

    RestaurantBusinessDayDomain createBusinessDay(RestaurantBusinessDayDomain day);

    RestaurantBusinessDayDomain reopenBusinessDay(UUID businessDayId, UUID userId);

    RestaurantBusinessDayDomain closeBusinessDay(UUID businessDayId, UUID userId);

    void saveBusinessDayEvent(UUID restaurantId, UUID businessDayId, BusinessDayEventType type,
            UUID userId, String reason);

    RestaurantBusinessDayDomain loadBusinessDayBlockers(UUID restaurantId, UUID businessDayId);

    BusinessDayClosureDomain createBusinessDayClosure(BusinessDayClosureDomain closure);

    List<PaymentTotalSnapshot> calculateBusinessDayPaymentTotals(UUID businessDayId);

    BigDecimal sumBusinessDayMovements(UUID businessDayId, String direction);

    BigDecimal sumBusinessDayCountedCash(UUID businessDayId);

    BigDecimal sumBusinessDayExpectedCash(UUID businessDayId);

    List<CashMovementDomain> findMovements(UUID restaurantId, UUID sessionId);

    CashMovementDomain saveMovement(CashMovementDomain movement);

    Optional<CashMovementDomain> findMovement(UUID restaurantId, UUID sessionId, UUID movementId);

    CashMovementDomain voidMovement(UUID movementId, UUID userId, UUID deviceId, String reason);

    CashSessionReconciliationDomain calculateSessionReconciliation(UUID restaurantId, UUID sessionId);

    Optional<CashSessionReconciliationDomain> findSessionClosure(UUID restaurantId, UUID sessionId);

    CashSessionReconciliationDomain saveSessionClosure(CashSessionReconciliationDomain reconciliation);

    void lockSession(UUID restaurantId, UUID sessionId);
}
