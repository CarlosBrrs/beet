package com.beet.backend.modules.cash.domain.api;

import com.beet.backend.modules.cash.domain.model.CashSessionDomain;

import java.math.BigDecimal;
import java.util.UUID;

public interface CashSessionServicePort {
    CashSessionDomain openSession(UUID restaurantId, UUID cashRegisterId, UUID userId,
            UUID deviceId, BigDecimal openingAmount, String notes);

    CashSessionDomain closeSession(UUID restaurantId, UUID sessionId, UUID userId,
            UUID deviceId, BigDecimal closingAmount, String notes);

    CashSessionDomain forceCloseSession(UUID restaurantId, UUID sessionId, UUID userId,
            UUID deviceId, BigDecimal closingAmount, String notes);

    CashSessionDomain rebindSession(UUID restaurantId, UUID sessionId, UUID userId,
            UUID newDeviceId);

    CashSessionDomain getActiveSession(UUID restaurantId, UUID deviceId);
}
