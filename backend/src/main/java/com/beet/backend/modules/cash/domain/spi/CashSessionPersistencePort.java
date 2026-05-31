package com.beet.backend.modules.cash.domain.spi;

import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.cash.domain.model.CashSessionSummary;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashSessionPersistencePort {
    CashSessionDomain create(CashSessionDomain session);

    CashSessionDomain close(UUID sessionId, UUID closedBy, UUID closedDeviceId,
            BigDecimal closingAmount, String notes);

    CashSessionDomain rebind(UUID sessionId, UUID newDeviceId);

    Optional<CashSessionDomain> findSessionById(UUID id);

    Optional<CashSessionDomain> findOpenByRegisterId(UUID cashRegisterId);

    Optional<CashSessionDomain> findOpenByRestaurantAndDevice(UUID restaurantId, UUID deviceId);

    PageResponse<CashSessionSummary> findSessionsPaged(
            List<UUID> restaurantIds,
            CashSessionStatus status,
            LocalDate from,
            LocalDate to,
            String timeZone,
            UUID cashRegisterId,
            int page,
            int size);
}
