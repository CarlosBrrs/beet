package com.beet.backend.modules.cash.domain.api;

import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.cash.domain.model.CashSessionSummary;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CashSessionQueryPort {
    CashSessionDomain getActiveSession(UUID restaurantId, UUID deviceId);

    PageResponse<CashSessionSummary> listSessions(
            List<UUID> restaurantIds,
            CashSessionStatus status,
            LocalDate from,
            LocalDate to,
            String timeZone,
            UUID cashRegisterId,
            int page,
            int size);
}
