package com.beet.backend.modules.cash.application.handler;

import com.beet.backend.modules.cash.application.dto.BusinessDayClosureResponse;
import com.beet.backend.modules.cash.application.dto.BusinessDayResponse;
import com.beet.backend.modules.cash.application.dto.CashMovementRequest;
import com.beet.backend.modules.cash.application.dto.CashMovementResponse;
import com.beet.backend.modules.cash.application.dto.CashSessionReconciliationResponse;
import com.beet.backend.modules.cash.application.dto.CloseCashSessionRequest;
import com.beet.backend.modules.cash.application.dto.PaymentTotalResponse;
import com.beet.backend.modules.cash.domain.api.CashOperationsServicePort;
import com.beet.backend.modules.cash.domain.model.BusinessDayClosureDomain;
import com.beet.backend.modules.cash.domain.model.CashMovementDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionReconciliationDomain;
import com.beet.backend.modules.cash.domain.model.PaymentTotalSnapshot;
import com.beet.backend.modules.cash.domain.model.RestaurantBusinessDayDomain;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.security.DeviceContext;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashOperationsHandlerImpl implements CashOperationsHandler {
    private final CashOperationsServicePort service;
    private final DeviceContext deviceContext;

    @Override
    public ApiGenericResponse<BusinessDayResponse> currentBusinessDay(UUID restaurantId) {
        RestaurantBusinessDayDomain current = service.getCurrentBusinessDay(restaurantId);
        return ApiGenericResponse.success(current == null ? null : toResponse(current));
    }

    @Override
    public ApiGenericResponse<List<BusinessDayResponse>> listBusinessDays(UUID restaurantId) {
        return ApiGenericResponse.success(service.listBusinessDays(restaurantId).stream()
                .map(this::toResponse)
                .toList());
    }

    @Override
    public ApiGenericResponse<BusinessDayResponse> getBusinessDay(UUID restaurantId, UUID businessDayId) {
        return ApiGenericResponse.success(toResponse(service.getBusinessDay(restaurantId, businessDayId)));
    }

    @Override
    public ApiGenericResponse<BusinessDayResponse> openBusinessDay(UUID restaurantId) {
        return ApiGenericResponse.success(toResponse(service.openBusinessDay(
                restaurantId, SecurityUtils.getAuthenticatedUserId())));
    }

    @Override
    public ApiGenericResponse<BusinessDayClosureResponse> closeBusinessDay(
            UUID restaurantId, UUID businessDayId, String notes) {
        return ApiGenericResponse.success(toResponse(service.closeBusinessDay(
                restaurantId, businessDayId, SecurityUtils.getAuthenticatedUserId(), notes)));
    }

    @Override
    public ApiGenericResponse<BusinessDayResponse> reopenBusinessDay(
            UUID restaurantId, UUID businessDayId, String reason) {
        return ApiGenericResponse.success(toResponse(service.reopenBusinessDay(
                restaurantId, businessDayId, SecurityUtils.getAuthenticatedUserId(), reason)));
    }

    @Override
    public ApiGenericResponse<List<CashMovementResponse>> listMovements(UUID restaurantId, UUID sessionId) {
        return ApiGenericResponse.success(service.listMovements(restaurantId, sessionId).stream()
                .map(this::toResponse)
                .toList());
    }

    @Override
    public ApiGenericResponse<CashMovementResponse> recordMovement(
            UUID restaurantId, UUID sessionId, CashMovementRequest request) {
        CashMovementDomain movement = CashMovementDomain.builder()
                .restaurantId(restaurantId)
                .cashSessionId(sessionId)
                .direction(request.direction())
                .reason(request.reason())
                .amount(request.amount())
                .notes(request.notes())
                .build();
        return ApiGenericResponse.success(toResponse(service.recordMovement(
                movement, SecurityUtils.getAuthenticatedUserId(), deviceContext.getDeviceId())));
    }

    @Override
    public ApiGenericResponse<CashMovementResponse> voidMovement(
            UUID restaurantId, UUID sessionId, UUID movementId, String reason) {
        return ApiGenericResponse.success(toResponse(service.voidMovement(
                restaurantId, sessionId, movementId,
                SecurityUtils.getAuthenticatedUserId(), deviceContext.getDeviceId(), reason)));
    }

    @Override
    public ApiGenericResponse<CashSessionReconciliationResponse> reconciliation(
            UUID restaurantId, UUID sessionId) {
        return ApiGenericResponse.success(toResponse(service.getReconciliation(restaurantId, sessionId)));
    }

    @Override
    public ApiGenericResponse<CashSessionReconciliationResponse> closeSession(
            UUID restaurantId, UUID sessionId, CloseCashSessionRequest request, boolean force) {
        return ApiGenericResponse.success(toResponse(service.closeSession(
                restaurantId,
                sessionId,
                SecurityUtils.getAuthenticatedUserId(),
                deviceContext.getDeviceId(),
                request.countedCash(),
                request.differenceReason(),
                request.notes(),
                force)));
    }

    private BusinessDayResponse toResponse(RestaurantBusinessDayDomain day) {
        return new BusinessDayResponse(
                day.getId(), day.getRestaurantId(), day.getBusinessDate(), day.getTimeZoneSnapshot(),
                day.getStatus(), day.getOpenedAt(), day.getOpenedBy(), day.getClosedAt(), day.getClosedBy(),
                day.getOpenSessionCount(), day.getPendingOrderCount(), day.getMissingReconciliationCount(),
                day.getUnexplainedDifferenceCount());
    }

    private CashMovementResponse toResponse(CashMovementDomain movement) {
        return new CashMovementResponse(
                movement.getId(), movement.getRestaurantId(), movement.getBusinessDayId(),
                movement.getCashSessionId(), movement.getDirection(), movement.getReason(),
                movement.getAmount(), movement.getStatus(), movement.getNotes(), movement.getCreatedAt(),
                movement.getCreatedBy(), movement.getCreatedDeviceId(), movement.getVoidedAt(),
                movement.getVoidedBy(), movement.getVoidReason());
    }

    private CashSessionReconciliationResponse toResponse(CashSessionReconciliationDomain value) {
        return new CashSessionReconciliationResponse(
                value.getId(), value.getRestaurantId(), value.getBusinessDayId(), value.getCashSessionId(),
                value.getOpeningAmount(), value.getPaymentsTotal(), value.getTipsTotal(), value.getRefundsTotal(),
                value.getCashInTotal(), value.getCashOutTotal(), value.getExpectedCash(), value.getCountedCash(),
                value.getDifferenceAmount(), value.getDifferenceReason(), value.getNotes(), value.getClosedAt(),
                value.getClosedBy(), value.getClosedDeviceId(), value.isBlind(), value.isClosed(),
                totals(value.getPaymentTotals()));
    }

    private BusinessDayClosureResponse toResponse(BusinessDayClosureDomain value) {
        return new BusinessDayClosureResponse(
                value.getId(), value.getRestaurantId(), value.getBusinessDayId(), value.getClosureSequence(),
                value.getPaymentsTotal(), value.getTipsTotal(), value.getRefundsTotal(), value.getCashInTotal(),
                value.getCashOutTotal(), value.getExpectedCashTotal(), value.getCountedCashTotal(),
                value.getDifferenceTotal(), value.getNotes(), value.getClosedAt(), value.getClosedBy(),
                totals(value.getPaymentTotals()));
    }

    private List<PaymentTotalResponse> totals(List<PaymentTotalSnapshot> values) {
        return values == null ? List.of() : values.stream()
                .map(value -> new PaymentTotalResponse(
                        value.paymentMethodId(), value.methodCode(), value.methodName(), value.methodType(),
                        value.paymentCount(), value.paymentAmount(), value.tipAmount(),
                        value.refundAmount(), value.netAmount()))
                .toList();
    }
}
