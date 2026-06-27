package com.beet.backend.modules.table.domain.usecase;

import com.beet.backend.modules.table.domain.api.RestaurantTableServicePort;
import com.beet.backend.modules.table.domain.exception.RestaurantTableAlreadyExistsException;
import com.beet.backend.modules.table.domain.exception.RestaurantTableNotFoundException;
import com.beet.backend.modules.table.domain.exception.RestaurantTableOccupiedException;
import com.beet.backend.modules.table.domain.model.RestaurantTableDomain;
import com.beet.backend.modules.table.domain.spi.RestaurantTablePersistencePort;
import com.beet.backend.modules.table.domain.spi.RestaurantTableRestaurantGateway;
import com.beet.backend.shared.domain.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantTableUseCase implements RestaurantTableServicePort {
    private final RestaurantTablePersistencePort persistence;
    private final RestaurantTableRestaurantGateway restaurantGateway;

    @Override
    @Transactional
    public RestaurantTableDomain create(RestaurantTableDomain table) {
        normalize(table);
        validateCapacity(table.getRestaurantId(), table.getCapacity());
        if (persistence.existsByName(table.getRestaurantId(), table.getName())) {
            throw RestaurantTableAlreadyExistsException.forName(table.getName());
        }
        table.setIsActive(true);
        return persistence.save(table);
    }

    @Override
    @Transactional
    public RestaurantTableDomain update(RestaurantTableDomain changes) {
        RestaurantTableDomain existing = loadForUpdate(changes.getRestaurantId(), changes.getId());
        String updatedName = changes.getName() != null ? requireName(changes.getName()) : existing.getName();
        Integer updatedCapacity = changes.getCapacity() != null ? changes.getCapacity() : existing.getCapacity();
        validateCapacity(existing.getRestaurantId(), updatedCapacity);
        if (!existing.getName().equalsIgnoreCase(updatedName)
                && persistence.existsByName(existing.getRestaurantId(), updatedName)) {
            throw RestaurantTableAlreadyExistsException.forName(updatedName);
        }

        existing.setName(updatedName);
        existing.setCapacity(updatedCapacity);
        if (changes.getArea() != null) existing.setArea(normalizeOptional(changes.getArea()));
        if (changes.getSortOrder() != null) existing.setSortOrder(requireNonNegative(changes.getSortOrder()));
        if (changes.getNotes() != null) existing.setNotes(normalizeOptional(changes.getNotes()));
        if (changes.getIsActive() != null) {
            if (!changes.getIsActive()) ensureAvailable(existing.getRestaurantId(), existing.getId());
            existing.setIsActive(changes.getIsActive());
        }
        existing.setUpdatedBy(changes.getUpdatedBy());
        return persistence.update(existing);
    }

    @Override
    @Transactional
    public RestaurantTableDomain deactivate(UUID restaurantId, UUID tableId, UUID updatedBy) {
        RestaurantTableDomain existing = loadForUpdate(restaurantId, tableId);
        ensureAvailable(restaurantId, tableId);
        existing.setIsActive(false);
        existing.setUpdatedBy(updatedBy);
        return persistence.update(existing);
    }

    @Override
    public List<RestaurantTableDomain> listByRestaurant(UUID restaurantId) {
        return persistence.findByRestaurantId(restaurantId);
    }

    @Override
    @Transactional
    public void validateAvailableForOrder(UUID restaurantId, UUID tableId) {
        RestaurantTableDomain table = loadForUpdate(restaurantId, tableId);
        if (!Boolean.TRUE.equals(table.getIsActive())) {
            throw new IllegalArgumentException("Restaurant table is inactive.");
        }
        ensureAvailable(restaurantId, tableId);
    }

    private RestaurantTableDomain loadForUpdate(UUID restaurantId, UUID tableId) {
        RestaurantTableDomain table = persistence.findByIdForUpdate(tableId)
                .orElseThrow(() -> RestaurantTableNotFoundException.forId(tableId));
        if (!restaurantId.equals(table.getRestaurantId())) {
            throw new AccessDeniedException("Restaurant table does not belong to restaurant");
        }
        return table;
    }

    private void ensureAvailable(UUID restaurantId, UUID tableId) {
        if (persistence.findOpenOrderId(restaurantId, tableId).isPresent()) {
            throw RestaurantTableOccupiedException.forId(tableId);
        }
    }

    private void normalize(RestaurantTableDomain table) {
        table.setName(requireName(table.getName()));
        table.setArea(normalizeOptional(table.getArea()));
        table.setNotes(normalizeOptional(table.getNotes()));
        table.setSortOrder(table.getSortOrder() != null ? requireNonNegative(table.getSortOrder()) : 0);
    }

    private void validateCapacity(UUID restaurantId, Integer capacity) {
        if (capacity == null || capacity <= 0) {
            throw new IllegalArgumentException("Restaurant table capacity must be greater than zero.");
        }
        Integer maxCapacity = restaurantGateway.getMaxTableCapacity(restaurantId);
        if (maxCapacity != null && capacity > maxCapacity) {
            throw new IllegalArgumentException("Restaurant table capacity exceeds restaurant maximum: " + maxCapacity);
        }
    }

    private int requireNonNegative(int sortOrder) {
        if (sortOrder < 0) {
            throw new IllegalArgumentException("Restaurant table sort order must be greater than or equal to zero.");
        }
        return sortOrder;
    }

    private String requireName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant table name is required.");
        }
        return name.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
