package com.beet.backend.modules.cash.application.handler;

import com.beet.backend.modules.cash.application.dto.CashRegisterResponse;
import com.beet.backend.modules.cash.application.dto.CashSessionListResponse;
import com.beet.backend.modules.cash.application.dto.CashSessionResponse;
import com.beet.backend.modules.cash.application.dto.CloseCashSessionRequest;
import com.beet.backend.modules.cash.application.dto.CreateCashRegisterRequest;
import com.beet.backend.modules.cash.application.dto.OpenCashSessionRequest;
import com.beet.backend.modules.cash.application.dto.RebindCashSessionRequest;
import com.beet.backend.modules.cash.application.dto.UpdateCashRegisterRequest;
import com.beet.backend.modules.cash.domain.api.CashRegisterServicePort;
import com.beet.backend.modules.cash.domain.api.CashSessionQueryPort;
import com.beet.backend.modules.cash.domain.api.CashSessionServicePort;
import com.beet.backend.modules.cash.domain.exception.CashSessionNotFoundException;
import com.beet.backend.modules.cash.domain.model.CashRegisterDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.cash.domain.model.CashSessionSummary;
import com.beet.backend.modules.restaurant.domain.api.RestaurantServicePort;
import com.beet.backend.modules.role.application.dto.UserPermissionEntry;
import com.beet.backend.modules.role.domain.model.PermissionAction;
import com.beet.backend.modules.role.domain.model.PermissionModule;
import com.beet.backend.modules.role.domain.spi.RolePersistencePort;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.DeviceContext;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashHandlerImpl implements CashHandler {

    private final CashRegisterServicePort registerService;
    private final CashSessionServicePort sessionService;
    private final CashSessionQueryPort sessionQuery;
    private final RestaurantServicePort restaurantService;
    private final RolePersistencePort rolePersistence;
    private final DeviceContext deviceContext;

    @Override
    public ApiGenericResponse<CashRegisterResponse> createRegister(
            UUID restaurantId, CreateCashRegisterRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        CashRegisterDomain created = registerService.create(CashRegisterDomain.builder()
                .restaurantId(restaurantId)
                .name(request.name())
                .deviceId(request.deviceId())
                .notes(request.notes())
                .isActive(true)
                .createdBy(userId)
                .updatedBy(userId)
                .build());
        return ApiGenericResponse.success(toRegisterResponse(created));
    }

    @Override
    public ApiGenericResponse<List<CashRegisterResponse>> listRegisters(UUID restaurantId) {
        return ApiGenericResponse.success(registerService.listByRestaurant(restaurantId)
                .stream()
                .map(this::toRegisterResponse)
                .toList());
    }

    @Override
    public ApiGenericResponse<CashRegisterResponse> updateRegister(
            UUID restaurantId, UUID registerId, UpdateCashRegisterRequest request) {
        CashRegisterDomain updated = registerService.update(CashRegisterDomain.builder()
                .id(registerId)
                .restaurantId(restaurantId)
                .name(request.name())
                .deviceId(request.deviceId())
                .isActive(request.isActive())
                .notes(request.notes())
                .updatedBy(SecurityUtils.getAuthenticatedUserId())
                .build());
        return ApiGenericResponse.success(toRegisterResponse(updated));
    }

    @Override
    public ApiGenericResponse<CashRegisterResponse> deactivateRegister(UUID restaurantId, UUID registerId) {
        CashRegisterDomain deactivated = registerService.deactivate(
                restaurantId,
                registerId,
                SecurityUtils.getAuthenticatedUserId());
        return ApiGenericResponse.success(toRegisterResponse(deactivated));
    }

    @Override
    public ApiGenericResponse<CashSessionResponse> openSession(UUID restaurantId, OpenCashSessionRequest request) {
        CashSessionDomain opened = sessionService.openSession(
                restaurantId,
                request.cashRegisterId(),
                SecurityUtils.getAuthenticatedUserId(),
                deviceContext.getDeviceId(),
                request.openingAmount(),
                request.notes());
        return ApiGenericResponse.success(toSessionResponse(opened));
    }

    @Override
    public ApiGenericResponse<CashSessionResponse> closeSession(
            UUID restaurantId, UUID sessionId, CloseCashSessionRequest request) {
        CashSessionDomain closed = sessionService.closeSession(
                restaurantId,
                sessionId,
                SecurityUtils.getAuthenticatedUserId(),
                deviceContext.getDeviceId(),
                request.closingAmount(),
                request.notes());
        return ApiGenericResponse.success(toSessionResponse(closed));
    }

    @Override
    public ApiGenericResponse<CashSessionResponse> forceCloseSession(
            UUID restaurantId, UUID sessionId, CloseCashSessionRequest request) {
        CashSessionDomain closed = sessionService.forceCloseSession(
                restaurantId,
                sessionId,
                SecurityUtils.getAuthenticatedUserId(),
                deviceContext.getDeviceId(),
                request.closingAmount(),
                request.notes());
        return ApiGenericResponse.success(toSessionResponse(closed));
    }

    @Override
    public ApiGenericResponse<CashSessionResponse> rebindSession(
            UUID restaurantId, UUID sessionId, RebindCashSessionRequest request) {
        CashSessionDomain rebound = sessionService.rebindSession(
                restaurantId,
                sessionId,
                SecurityUtils.getAuthenticatedUserId(),
                request.newDeviceId());
        return ApiGenericResponse.success(toSessionResponse(rebound));
    }

    @Override
    public ApiGenericResponse<CashSessionResponse> getActiveSession(UUID restaurantId) {
        try {
            return ApiGenericResponse.success(toSessionResponse(
                    sessionQuery.getActiveSession(restaurantId, deviceContext.getDeviceId())));
        } catch (CashSessionNotFoundException exception) {
            return ApiGenericResponse.success(null);
        }
    }

    @Override
    public ApiGenericResponse<PageResponse<CashSessionListResponse>> listSessions(
            UUID restaurantId,
            CashSessionStatus status,
            LocalDate from,
            LocalDate to,
            String timeZone,
            UUID cashRegisterId,
            int page,
            int size) {
        validateDateRange(from, to);
        String validatedTimeZone = validateTimeZone(timeZone);
        return ApiGenericResponse.success(toListResponse(sessionQuery.listSessions(
                List.of(restaurantId),
                status,
                from,
                to,
                validatedTimeZone,
                cashRegisterId,
                normalizePage(page),
                normalizeSize(size))));
    }

    @Override
    public ApiGenericResponse<PageResponse<CashSessionListResponse>> listAccountSessions(
            UUID restaurantId,
            CashSessionStatus status,
            LocalDate from,
            LocalDate to,
            String timeZone,
            UUID cashRegisterId,
            int page,
            int size) {
        validateDateRange(from, to);
        String validatedTimeZone = validateTimeZone(timeZone);
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        List<UUID> restaurantIds = resolveAccountRestaurantIds(userId);
        if (restaurantId != null) {
            restaurantIds = restaurantIds.contains(restaurantId) ? List.of(restaurantId) : List.of();
        }
        return ApiGenericResponse.success(toListResponse(sessionQuery.listSessions(
                restaurantIds,
                status,
                from,
                to,
                validatedTimeZone,
                cashRegisterId,
                normalizePage(page),
                normalizeSize(size))));
    }

    private CashRegisterResponse toRegisterResponse(CashRegisterDomain register) {
        return new CashRegisterResponse(
                register.getId(),
                register.getRestaurantId(),
                register.getName(),
                register.getDeviceId(),
                Boolean.TRUE.equals(register.getIsActive()),
                register.getNotes(),
                register.getCreatedAt(),
                register.getUpdatedAt());
    }

    private CashSessionResponse toSessionResponse(CashSessionDomain session) {
        return new CashSessionResponse(
                session.getId(),
                session.getRestaurantId(),
                session.getCashRegisterId(),
                session.getStatus(),
                session.getOpenedAt(),
                session.getOpenedBy(),
                session.getOpenedDeviceId(),
                session.getOpeningAmount(),
                session.getClosedAt(),
                session.getClosedBy(),
                session.getClosedDeviceId(),
                session.getClosingAmount(),
                session.getNotes());
    }

    private PageResponse<CashSessionListResponse> toListResponse(PageResponse<CashSessionSummary> page) {
        List<CashSessionListResponse> content = page.content().stream()
                .map(this::toListResponse)
                .toList();
        return PageResponse.of(content, page.totalElements(), page.number(), page.size());
    }

    private CashSessionListResponse toListResponse(CashSessionSummary session) {
        return new CashSessionListResponse(
                session.id(),
                session.restaurantId(),
                session.restaurantName(),
                session.cashRegisterId(),
                session.cashRegisterName(),
                session.status(),
                session.openedAt(),
                session.openedBy(),
                session.openedDeviceId(),
                session.openingAmount(),
                session.closedAt(),
                session.closedBy(),
                session.closedDeviceId(),
                session.closingAmount(),
                session.notes());
    }

    private List<UUID> resolveAccountRestaurantIds(UUID userId) {
        List<UserPermissionEntry> permissions = rolePersistence.findAllPermissionsForUser(userId);
        if (permissions.stream().anyMatch(this::hasGlobalAccess)) {
            return restaurantService.getRestaurantsWithRole(userId).stream()
                    .map(restaurant -> restaurant.id())
                    .toList();
        }
        return permissions.stream()
                .filter(permission -> permission.restaurantId() != null)
                .filter(this::hasCashViewAccess)
                .map(UserPermissionEntry::restaurantId)
                .distinct()
                .toList();
    }

    private boolean hasGlobalAccess(UserPermissionEntry permission) {
        List<PermissionAction> actions = permission.permissions().get(PermissionModule.ALL);
        return permission.restaurantId() == null
                && actions != null
                && actions.contains(PermissionAction.ALL);
    }

    private boolean hasCashViewAccess(UserPermissionEntry permission) {
        List<PermissionAction> actions = permission.permissions().get(PermissionModule.CASH);
        return actions != null
                && (actions.contains(PermissionAction.VIEW)
                        || actions.contains(PermissionAction.MANAGE)
                        || actions.contains(PermissionAction.ALL));
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from date must be before or equal to to date");
        }
    }

    private String validateTimeZone(String timeZone) {
        try {
            return ZoneId.of(timeZone).getId();
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid time zone: " + timeZone);
        }
    }
}
