package com.beet.backend.modules.table.domain.usecase;

import com.beet.backend.modules.table.domain.exception.RestaurantTableAlreadyExistsException;
import com.beet.backend.modules.table.domain.exception.RestaurantTableOccupiedException;
import com.beet.backend.modules.table.domain.model.RestaurantTableDomain;
import com.beet.backend.modules.table.domain.spi.RestaurantTablePersistencePort;
import com.beet.backend.modules.table.domain.spi.RestaurantTableRestaurantGateway;
import com.beet.backend.shared.domain.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantTableUseCaseTest {

    @Mock
    private RestaurantTablePersistencePort persistence;

    @Mock
    private RestaurantTableRestaurantGateway restaurantGateway;

    @InjectMocks
    private RestaurantTableUseCase useCase;

    @Test
    void shouldCreateTableWithinRestaurantCapacity() {
        UUID restaurantId = UUID.randomUUID();
        RestaurantTableDomain table = RestaurantTableDomain.builder()
                .restaurantId(restaurantId)
                .name(" Table 01 ")
                .capacity(4)
                .area(" ")
                .build();

        when(restaurantGateway.getMaxTableCapacity(restaurantId)).thenReturn(6);
        when(persistence.existsByName(restaurantId, "Table 01")).thenReturn(false);
        when(persistence.save(table)).thenReturn(table);

        RestaurantTableDomain created = useCase.create(table);

        assertEquals("Table 01", created.getName());
        assertEquals(0, created.getSortOrder());
        assertNull(created.getArea());
        assertEquals(true, created.getIsActive());
    }

    @Test
    void shouldRejectCapacityAboveRestaurantMaximum() {
        UUID restaurantId = UUID.randomUUID();
        RestaurantTableDomain table = RestaurantTableDomain.builder()
                .restaurantId(restaurantId)
                .name("Table 01")
                .capacity(7)
                .build();

        when(restaurantGateway.getMaxTableCapacity(restaurantId)).thenReturn(6);

        assertThrows(IllegalArgumentException.class, () -> useCase.create(table));
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateNameWithinRestaurant() {
        UUID restaurantId = UUID.randomUUID();
        RestaurantTableDomain table = RestaurantTableDomain.builder()
                .restaurantId(restaurantId)
                .name("Table 01")
                .capacity(4)
                .build();

        when(restaurantGateway.getMaxTableCapacity(restaurantId)).thenReturn(6);
        when(persistence.existsByName(restaurantId, "Table 01")).thenReturn(true);

        assertThrows(RestaurantTableAlreadyExistsException.class, () -> useCase.create(table));
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldRejectBlankNameAfterNormalization() {
        RestaurantTableDomain table = RestaurantTableDomain.builder()
                .restaurantId(UUID.randomUUID())
                .name("   ")
                .capacity(4)
                .build();

        assertThrows(IllegalArgumentException.class, () -> useCase.create(table));
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldRejectDeactivateWhenTableHasOpenOrder() {
        UUID restaurantId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        RestaurantTableDomain table = table(tableId, restaurantId, true);

        when(persistence.findByIdForUpdate(tableId)).thenReturn(Optional.of(table));
        when(persistence.findOpenOrderId(restaurantId, tableId)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThrows(RestaurantTableOccupiedException.class, () ->
                useCase.deactivate(restaurantId, tableId, UUID.randomUUID()));

        verify(persistence, never()).update(any());
    }

    @Test
    void shouldRejectTableFromAnotherRestaurant() {
        UUID tableId = UUID.randomUUID();
        RestaurantTableDomain table = table(tableId, UUID.randomUUID(), true);

        when(persistence.findByIdForUpdate(tableId)).thenReturn(Optional.of(table));

        assertThrows(AccessDeniedException.class, () ->
                useCase.validateAvailableForOrder(UUID.randomUUID(), tableId));

        verify(persistence, never()).findOpenOrderId(any(), any());
    }

    @Test
    void shouldReactivateInactiveTable() {
        UUID restaurantId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        RestaurantTableDomain table = table(tableId, restaurantId, false);
        RestaurantTableDomain changes = RestaurantTableDomain.builder()
                .id(tableId)
                .restaurantId(restaurantId)
                .isActive(true)
                .updatedBy(UUID.randomUUID())
                .build();

        when(persistence.findByIdForUpdate(tableId)).thenReturn(Optional.of(table));
        when(restaurantGateway.getMaxTableCapacity(restaurantId)).thenReturn(6);
        when(persistence.update(table)).thenReturn(table);

        RestaurantTableDomain updated = useCase.update(changes);

        assertEquals(true, updated.getIsActive());
        verify(persistence).update(table);
    }

    private RestaurantTableDomain table(UUID tableId, UUID restaurantId, boolean isActive) {
        return RestaurantTableDomain.builder()
                .id(tableId)
                .restaurantId(restaurantId)
                .name("Table 01")
                .capacity(4)
                .sortOrder(0)
                .isActive(isActive)
                .build();
    }
}
