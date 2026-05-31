package com.beet.backend.modules.order.domain.usecase;

import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import com.beet.backend.modules.menu.domain.spi.SubmenuNodeQueryPort;
import com.beet.backend.modules.order.domain.exception.OrderNotFoundException;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.spi.OrderPersistencePort;
import com.beet.backend.modules.order.domain.spi.OrderTaxQueryPort;
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
}
