package com.beet.backend.modules.restaurant.domain.usecase;

import com.beet.backend.modules.order.domain.api.OrderServicePort;
import com.beet.backend.modules.restaurant.domain.api.RestaurantServicePort;
import com.beet.backend.modules.restaurant.domain.exception.RestaurantAlreadyExistsException;
import com.beet.backend.modules.restaurant.domain.exception.RestaurantLimitExceededException;
import com.beet.backend.modules.restaurant.domain.exception.RestaurantNotFoundException;
import com.beet.backend.modules.restaurant.domain.exception.RoleAssignmentException;
import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.model.RestaurantSettings;
import com.beet.backend.modules.restaurant.domain.model.RestaurantWithRole;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantIdentityGateway;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPersistencePort;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantSubscriptionGateway;
import com.beet.backend.modules.role.domain.model.UserRoleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantUseCase implements RestaurantServicePort {
    private static final int DEFAULT_PREPAID_ORDER_EXPIRATION_MINUTES = 30;
    private static final int MIN_PREPAID_ORDER_EXPIRATION_MINUTES = 5;
    private static final int MAX_PREPAID_ORDER_EXPIRATION_MINUTES = 1440;
    private static final int DEFAULT_MAX_TABLE_CAPACITY = 1;
    private static final String DEFAULT_TIME_ZONE = "America/Bogota";

    private final RestaurantPersistencePort persistencePort;
    private final RestaurantSubscriptionGateway subscriptionGateway;
    private final RestaurantIdentityGateway identityGateway;
    private final OrderServicePort orderService;

    @Override
    @Transactional
    public RestaurantWithRole create(RestaurantDomain domain) {
        domain.setSettings(normalizeSettings(domain.getSettings()));

        int maxAllowed = subscriptionGateway.getMaxRestaurantsAllowed(domain.getOwnerId());
        int currentCount = persistencePort.countByOwnerId(domain.getOwnerId());

        if (currentCount >= maxAllowed) {
            throw RestaurantLimitExceededException.forPlan(maxAllowed);
        }

        validateUniqueFields(domain, null);

        RestaurantDomain savedRestaurant = persistencePort.save(domain);

        try {
            identityGateway.assignRole(savedRestaurant.getOwnerId(), savedRestaurant.getId(), "Owner");
        } catch (Exception e) {
            throw RoleAssignmentException.forRole("Owner", e);
        }
        orderService.ensureDefaultPaymentMethods(savedRestaurant.getId(), savedRestaurant.getOwnerId());
        return withRole(savedRestaurant, "Owner");
    }

    @Override
    public RestaurantDomain getById(UUID id, UUID ownerId) {
        return persistencePort.findById(id)
                .filter(r -> r.getOwnerId().equals(ownerId))
                .orElseThrow(() -> RestaurantNotFoundException.forId(id));
    }

    @Override
    public RestaurantWithRole getByIdWithRole(UUID id, UUID userId) {
        RestaurantDomain restaurant = persistencePort.findById(id)
                .orElseThrow(() -> RestaurantNotFoundException.forId(id));
        String roleName = resolveRoleName(restaurant, userId);
        return withRole(restaurant, roleName);
    }

    @Override
    public List<RestaurantDomain> getRestaurantsByOwner(UUID ownerId) {
        return persistencePort.findAllByOwnerId(ownerId);
    }

    @Override
    public List<RestaurantWithRole> getRestaurantsWithRole(UUID userId) {
        List<UserRoleDTO> userRoles = identityGateway.getUserRoles(userId);

        if (userRoles.isEmpty()) {
            return List.of();
        }

        List<UUID> restaurantIds = userRoles.stream()
                .map(UserRoleDTO::restaurantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<RestaurantDomain> restaurants = persistencePort.findAllById(restaurantIds);

        return restaurants.stream()
                .map(restaurant -> {
                    String roleName = userRoles.stream()
                            .filter(ur -> restaurant.getId().equals(ur.restaurantId()))
                            .findFirst()
                            .map(UserRoleDTO::roleName)
                            .orElse("Unknown");
                    return withRole(restaurant, roleName);
                })
                .toList();
    }

    @Override
    @Transactional
    public RestaurantDomain update(RestaurantDomain domain) {
        RestaurantDomain existing = getById(domain.getId(), domain.getOwnerId());
        RestaurantDomain merged = mergeRestaurant(existing, domain);
        validateUniqueFields(merged, existing);
        return persistencePort.save(merged);
    }

    @Override
    @Transactional
    public RestaurantWithRole updateWithRole(RestaurantDomain domain, UUID userId) {
        RestaurantDomain existing = persistencePort.findById(domain.getId())
                .orElseThrow(() -> RestaurantNotFoundException.forId(domain.getId()));
        String roleName = resolveRoleName(existing, userId);
        RestaurantDomain merged = mergeRestaurant(existing, domain);
        validateUniqueFields(merged, existing);
        RestaurantDomain saved = persistencePort.save(merged);
        return withRole(saved, roleName);
    }

    @Override
    public boolean existsById(UUID id) {
        return persistencePort.existsById(id);
    }

    private String resolveRoleName(RestaurantDomain restaurant, UUID userId) {
        List<UserRoleDTO> userRoles = identityGateway.getUserRoles(userId);
        String roleName = userRoles.stream()
                .filter(ur -> restaurant.getId().equals(ur.restaurantId()))
                .findFirst()
                .map(UserRoleDTO::roleName)
                .orElse(null);
        if (roleName == null && restaurant.getOwnerId().equals(userId)) {
            roleName = "Owner";
        }
        if (roleName == null) {
            throw RestaurantNotFoundException.forId(restaurant.getId());
        }
        return roleName;
    }

    private RestaurantDomain mergeRestaurant(RestaurantDomain existing, RestaurantDomain incoming) {
        return existing.toBuilder()
                .name(incoming.getName() != null ? incoming.getName() : existing.getName())
                .address(incoming.getAddress() != null ? blankToNull(incoming.getAddress()) : existing.getAddress())
                .email(incoming.getEmail() != null ? blankToNull(incoming.getEmail()) : existing.getEmail())
                .phoneNumber(incoming.getPhoneNumber() != null ? blankToNull(incoming.getPhoneNumber()) : existing.getPhoneNumber())
                .operationMode(incoming.getOperationMode() != null ? incoming.getOperationMode() : existing.getOperationMode())
                .isActive(incoming.getIsActive() != null ? incoming.getIsActive() : existing.getIsActive())
                .ownerId(existing.getOwnerId())
                .settings(normalizeSettings(mergeSettings(existing.getSettings(), incoming.getSettings())))
                .build();
    }

    private void validateUniqueFields(RestaurantDomain candidate, RestaurantDomain existing) {
        if ((existing == null || !Objects.equals(candidate.getName(), existing.getName()))
                && persistencePort.existsByNameAndOwnerId(candidate.getName(), candidate.getOwnerId())) {
            throw RestaurantAlreadyExistsException.forField("name", candidate.getName());
        }
        if (candidate.getAddress() != null
                && (existing == null || !Objects.equals(candidate.getAddress(), existing.getAddress()))
                && persistencePort.existsByAddressAndOwnerId(candidate.getAddress(), candidate.getOwnerId())) {
            throw RestaurantAlreadyExistsException.forField("address", candidate.getAddress());
        }
        if (candidate.getPhoneNumber() != null
                && (existing == null || !Objects.equals(candidate.getPhoneNumber(), existing.getPhoneNumber()))
                && persistencePort.existsByPhoneNumberAndOwnerId(candidate.getPhoneNumber(), candidate.getOwnerId())) {
            throw RestaurantAlreadyExistsException.forField("phone number", candidate.getPhoneNumber());
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RestaurantSettings mergeSettings(RestaurantSettings existing, RestaurantSettings incoming) {
        if (incoming == null) {
            return existing;
        }
        if (existing == null) {
            return incoming;
        }
        return RestaurantSettings.builder()
                .prePaymentEnabled(incoming.prePaymentEnabled() != null ? incoming.prePaymentEnabled() : existing.prePaymentEnabled())
                .allowTakeaway(incoming.allowTakeaway() != null ? incoming.allowTakeaway() : existing.allowTakeaway())
                .allowDelivery(incoming.allowDelivery() != null ? incoming.allowDelivery() : existing.allowDelivery())
                .maxTableCapacity(incoming.maxTableCapacity() != null ? incoming.maxTableCapacity() : existing.maxTableCapacity())
                .taxApplyMode(incoming.taxApplyMode() != null ? incoming.taxApplyMode() : existing.taxApplyMode())
                .defaultTaxPercentage(incoming.defaultTaxPercentage() != null ? incoming.defaultTaxPercentage() : existing.defaultTaxPercentage())
                .timeZone(incoming.timeZone() != null ? incoming.timeZone() : existing.timeZone())
                .prepaidOrderExpirationMinutes(incoming.prepaidOrderExpirationMinutes() != null
                        ? incoming.prepaidOrderExpirationMinutes()
                        : existing.prepaidOrderExpirationMinutes())
                .cashCountMode(incoming.cashCountMode() != null ? incoming.cashCountMode() : existing.cashCountMode())
                .build();
    }

    private RestaurantSettings normalizeSettings(RestaurantSettings settings) {
        if (settings == null) {
            settings = RestaurantSettings.builder().build();
        }
        return RestaurantSettings.builder()
                .prePaymentEnabled(settings.prePaymentEnabled() == null ? false : settings.prePaymentEnabled())
                .allowTakeaway(settings.allowTakeaway() == null ? true : settings.allowTakeaway())
                .allowDelivery(settings.allowDelivery() == null ? true : settings.allowDelivery())
                .maxTableCapacity(normalizeMaxTableCapacity(settings.maxTableCapacity()))
                .taxApplyMode(settings.taxApplyMode() == null
                        ? RestaurantSettings.TaxApplyMode.PER_INVOICE
                        : settings.taxApplyMode())
                .defaultTaxPercentage(normalizeTaxPercentage(settings.defaultTaxPercentage()))
                .timeZone(normalizeTimeZone(settings.timeZone()))
                .prepaidOrderExpirationMinutes(normalizePrepaidExpiration(settings.prepaidOrderExpirationMinutes()))
                .cashCountMode(settings.cashCountMode() == null
                        ? RestaurantSettings.CashCountMode.BLIND
                        : settings.cashCountMode())
                .build();
    }

    private int normalizeMaxTableCapacity(Integer capacity) {
        int normalized = capacity == null ? DEFAULT_MAX_TABLE_CAPACITY : capacity;
        if (normalized < 1) {
            throw new IllegalArgumentException("Max table capacity must be at least 1.");
        }
        return normalized;
    }

    private BigDecimal normalizeTaxPercentage(BigDecimal percentage) {
        BigDecimal normalized = percentage == null ? BigDecimal.ZERO : percentage;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Default tax percentage must be greater than or equal to 0.");
        }
        return normalized;
    }

    private int normalizePrepaidExpiration(Integer minutes) {
        int normalized = minutes == null ? DEFAULT_PREPAID_ORDER_EXPIRATION_MINUTES : minutes;
        if (normalized < MIN_PREPAID_ORDER_EXPIRATION_MINUTES
                || normalized > MAX_PREPAID_ORDER_EXPIRATION_MINUTES) {
            throw new IllegalArgumentException(
                    "Prepaid order expiration must be between 5 and 1440 minutes.");
        }
        return normalized;
    }

    private String normalizeTimeZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return DEFAULT_TIME_ZONE;
        }
        try {
            return ZoneId.of(timeZone).getId();
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid time zone: " + timeZone);
        }
    }

    private RestaurantWithRole withRole(RestaurantDomain restaurant, String roleName) {
        return new RestaurantWithRole(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.getEmail(),
                restaurant.getPhoneNumber(),
                restaurant.getOperationMode(),
                restaurant.getIsActive(),
                restaurant.getOwnerId(),
                restaurant.getSettings(),
                roleName);
    }
}

