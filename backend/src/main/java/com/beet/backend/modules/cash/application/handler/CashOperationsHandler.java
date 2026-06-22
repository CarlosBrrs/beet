package com.beet.backend.modules.cash.application.handler;

import com.beet.backend.modules.cash.application.dto.BusinessDayClosureResponse;
import com.beet.backend.modules.cash.application.dto.BusinessDayResponse;
import com.beet.backend.modules.cash.application.dto.CashMovementRequest;
import com.beet.backend.modules.cash.application.dto.CashMovementResponse;
import com.beet.backend.modules.cash.application.dto.CashSessionReconciliationResponse;
import com.beet.backend.modules.cash.application.dto.CloseCashSessionRequest;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;

import java.util.List;
import java.util.UUID;

public interface CashOperationsHandler {
    ApiGenericResponse<BusinessDayResponse> currentBusinessDay(UUID restaurantId);

    ApiGenericResponse<List<BusinessDayResponse>> listBusinessDays(UUID restaurantId);

    ApiGenericResponse<BusinessDayResponse> getBusinessDay(UUID restaurantId, UUID businessDayId);

    ApiGenericResponse<BusinessDayResponse> openBusinessDay(UUID restaurantId);

    ApiGenericResponse<BusinessDayClosureResponse> closeBusinessDay(
            UUID restaurantId, UUID businessDayId, String notes);

    ApiGenericResponse<BusinessDayResponse> reopenBusinessDay(
            UUID restaurantId, UUID businessDayId, String reason);

    ApiGenericResponse<List<CashMovementResponse>> listMovements(UUID restaurantId, UUID sessionId);

    ApiGenericResponse<CashMovementResponse> recordMovement(
            UUID restaurantId, UUID sessionId, CashMovementRequest request);

    ApiGenericResponse<CashMovementResponse> voidMovement(
            UUID restaurantId, UUID sessionId, UUID movementId, String reason);

    ApiGenericResponse<CashSessionReconciliationResponse> reconciliation(
            UUID restaurantId, UUID sessionId);

    ApiGenericResponse<CashSessionReconciliationResponse> closeSession(
            UUID restaurantId, UUID sessionId, CloseCashSessionRequest request, boolean force);
}
