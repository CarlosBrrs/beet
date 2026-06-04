package com.beet.backend.modules.order.domain.usecase;

import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import com.beet.backend.modules.menu.domain.spi.SubmenuNodeQueryPort;
import com.beet.backend.modules.order.domain.exception.OrderNotFoundException;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.spi.OrderPersistencePort;
import com.beet.backend.modules.order.domain.spi.OrderTableGateway;
import com.beet.backend.modules.order.domain.spi.OrderTaxQueryPort;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderUseCaseTest {

    @Mock
    private OrderPersistencePort orderPersistence;

    @Mock
    private OrderTaxQueryPort orderTaxQuery;

    @Mock
    private RestaurantPersistencePort restaurantPersistence;

    @Mock
    private ItemPersistencePort itemPersistence;

    @Mock
    private SubmenuNodeQueryPort submenuNodeQuery;

    @Mock
    private OrderTableGateway tableGateway;

    @InjectMocks
    private OrderUseCase useCase;

    @Test
    void shouldRejectAddingItemToOrderFromAnotherRestaurant() {
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderDomain foreignOrder = OrderDomain.builder()
                .id(orderId)
                .restaurantId(UUID.randomUUID())
                .build();

        when(orderPersistence.findByIdWithItems(orderId)).thenReturn(Optional.of(foreignOrder));

        assertThrows(OrderNotFoundException.class, () -> useCase.addItem(
                restaurantId,
                orderId,
                OrderItemDomain.builder().build(),
                UUID.randomUUID()));

        verify(orderPersistence, never()).saveItem(any());
    }

    @Test
    void shouldRequireTableForDineInOrder() {
        UUID restaurantId = UUID.randomUUID();
        OrderDomain order = OrderDomain.builder()
                .restaurantId(restaurantId)
                .serviceType(ServiceType.DINE_IN)
                .build();

        when(restaurantPersistence.findById(restaurantId))
                .thenReturn(Optional.of(RestaurantDomain.builder().id(restaurantId).build()));

        assertThrows(IllegalArgumentException.class, () -> useCase.createOrder(order, UUID.randomUUID()));
        verify(tableGateway, never()).validateAvailableForOrder(any(), any());
    }

    @Test
    void shouldRejectTableForTakeoutOrder() {
        UUID restaurantId = UUID.randomUUID();
        OrderDomain order = OrderDomain.builder()
                .restaurantId(restaurantId)
                .serviceType(ServiceType.TAKEOUT)
                .tableId(UUID.randomUUID())
                .build();

        when(restaurantPersistence.findById(restaurantId))
                .thenReturn(Optional.of(RestaurantDomain.builder().id(restaurantId).build()));

        assertThrows(IllegalArgumentException.class, () -> useCase.createOrder(order, UUID.randomUUID()));
        verify(tableGateway, never()).validateAvailableForOrder(any(), any());
    }

    @Test
    void shouldValidateAvailableTableForDineInOrder() {
        UUID restaurantId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        OrderDomain order = OrderDomain.builder()
                .restaurantId(restaurantId)
                .serviceType(ServiceType.DINE_IN)
                .tableId(tableId)
                .build();

        when(restaurantPersistence.findById(restaurantId))
                .thenReturn(Optional.of(RestaurantDomain.builder().id(restaurantId).build()));

        assertThrows(IllegalArgumentException.class, () -> useCase.createOrder(order, UUID.randomUUID()));
        verify(tableGateway).validateAvailableForOrder(restaurantId, tableId);
    }
}
