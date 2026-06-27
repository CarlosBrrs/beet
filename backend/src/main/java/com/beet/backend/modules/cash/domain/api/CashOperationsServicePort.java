package com.beet.backend.modules.cash.domain.api;

import com.beet.backend.modules.cash.domain.model.BusinessDayClosureDomain;
import com.beet.backend.modules.cash.domain.model.CashMovementDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionReconciliationDomain;
import com.beet.backend.modules.cash.domain.model.RestaurantBusinessDayDomain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CashOperationsServicePort {
    RestaurantBusinessDayDomain getCurrentBusinessDay(UUID restaurantId);

    List<RestaurantBusinessDayDomain> listBusinessDays(UUID restaurantId);

    RestaurantBusinessDayDomain getBusinessDay(UUID restaurantId, UUID businessDayId);

    RestaurantBusinessDayDomain openBusinessDay(UUID restaurantId, UUID userId);

    BusinessDayClosureDomain closeBusinessDay(UUID restaurantId, UUID businessDayId, UUID userId, String notes);

    RestaurantBusinessDayDomain reopenBusinessDay(UUID restaurantId, UUID businessDayId, UUID userId, String reason);

    List<CashMovementDomain> listMovements(UUID restaurantId, UUID sessionId);

    CashMovementDomain recordMovement(CashMovementDomain movement, UUID userId, UUID deviceId);

    CashMovementDomain voidMovement(UUID restaurantId, UUID sessionId, UUID movementId,
            UUID userId, UUID deviceId, String reason);

    CashSessionReconciliationDomain getReconciliation(UUID restaurantId, UUID sessionId);

    CashSessionReconciliationDomain closeSession(UUID restaurantId, UUID sessionId,
            UUID userId, UUID deviceId, BigDecimal countedCash, String differenceReason,
            String notes, boolean force);
}
