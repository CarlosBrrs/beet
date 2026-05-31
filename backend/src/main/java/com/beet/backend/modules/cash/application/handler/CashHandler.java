package com.beet.backend.modules.cash.application.handler;

import com.beet.backend.modules.cash.application.dto.CashRegisterResponse;
import com.beet.backend.modules.cash.application.dto.CashSessionListResponse;
import com.beet.backend.modules.cash.application.dto.CashSessionResponse;
import com.beet.backend.modules.cash.application.dto.CloseCashSessionRequest;
import com.beet.backend.modules.cash.application.dto.CreateCashRegisterRequest;
import com.beet.backend.modules.cash.application.dto.OpenCashSessionRequest;
import com.beet.backend.modules.cash.application.dto.RebindCashSessionRequest;
import com.beet.backend.modules.cash.application.dto.UpdateCashRegisterRequest;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CashHandler {
    ApiGenericResponse<CashRegisterResponse> createRegister(UUID restaurantId, CreateCashRegisterRequest request);

    ApiGenericResponse<List<CashRegisterResponse>> listRegisters(UUID restaurantId);

    ApiGenericResponse<CashRegisterResponse> updateRegister(
            UUID restaurantId, UUID registerId, UpdateCashRegisterRequest request);

    ApiGenericResponse<CashRegisterResponse> deactivateRegister(UUID restaurantId, UUID registerId);

    ApiGenericResponse<CashSessionResponse> openSession(UUID restaurantId, OpenCashSessionRequest request);

    ApiGenericResponse<CashSessionResponse> closeSession(
            UUID restaurantId, UUID sessionId, CloseCashSessionRequest request);

    ApiGenericResponse<CashSessionResponse> forceCloseSession(
            UUID restaurantId, UUID sessionId, CloseCashSessionRequest request);

    ApiGenericResponse<CashSessionResponse> rebindSession(
            UUID restaurantId, UUID sessionId, RebindCashSessionRequest request);

    ApiGenericResponse<CashSessionResponse> getActiveSession(UUID restaurantId);

    ApiGenericResponse<PageResponse<CashSessionListResponse>> listSessions(
            UUID restaurantId,
            CashSessionStatus status,
            LocalDate from,
            LocalDate to,
            String timeZone,
            UUID cashRegisterId,
            int page,
            int size);

    ApiGenericResponse<PageResponse<CashSessionListResponse>> listAccountSessions(
            UUID restaurantId,
            CashSessionStatus status,
            LocalDate from,
            LocalDate to,
            String timeZone,
            UUID cashRegisterId,
            int page,
            int size);
}
