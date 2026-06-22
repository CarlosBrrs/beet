package com.beet.backend.modules.order.domain.usecase;

import com.beet.backend.modules.cash.domain.api.BusinessDayQueryPort;
import com.beet.backend.modules.cash.domain.api.CashSessionQueryPort;
import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.cash.domain.model.CashSessionStatus;
import com.beet.backend.modules.cash.domain.model.RestaurantBusinessDayDomain;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import com.beet.backend.modules.item.domain.api.RecipeCalculationServicePort;
import com.beet.backend.modules.menu.domain.spi.SubmenuNodeQueryPort;
import com.beet.backend.modules.order.domain.exception.OrderNotFoundException;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.PaymentDomain;
import com.beet.backend.modules.order.domain.spi.OrderPersistencePort;
import com.beet.backend.modules.order.domain.spi.OrderTableGateway;
import com.beet.backend.modules.order.domain.spi.OrderTaxQueryPort;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPersistencePort;
import com.beet.backend.modules.template.domain.spi.TemplatePersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;

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
    private RecipeCalculationServicePort recipeCalculationService;

    @Mock
    private TemplatePersistencePort templatePersistence;

    @Mock
    private SubmenuNodeQueryPort submenuNodeQuery;

    @Mock
    private OrderTableGateway tableGateway;

    @Mock
    private BusinessDayQueryPort businessDayQuery;

    @Mock
    private CashSessionQueryPort cashSessionQuery;

    @InjectMocks
    private OrderUseCase useCase;

    private UUID businessDayId;
    private UUID cashSessionId;

    @BeforeEach
    void setUpBusinessDay() {
        businessDayId = UUID.randomUUID();
        cashSessionId = UUID.randomUUID();
        lenient().when(businessDayQuery.requireOpenBusinessDay(any()))
                .thenReturn(RestaurantBusinessDayDomain.builder()
                        .id(businessDayId)
                        .businessDate(java.time.LocalDate.now())
                        .build());
        lenient().when(cashSessionQuery.getSessionForUpdate(any(), any()))
                .thenReturn(CashSessionDomain.builder()
                        .id(cashSessionId)
                        .businessDayId(businessDayId)
                        .status(CashSessionStatus.OPEN)
                        .build());
    }

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

    @Test
    void shouldRejectCompletionBeforeKitchenIsReady() {
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderDomain order = completableOrder(restaurantId, orderId);
        order.setKitchenStatus(KitchenStatus.PREPARING);
        when(orderPersistence.findByIdWithItems(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.completeOrder(restaurantId, orderId, UUID.randomUUID()));

        verify(orderPersistence, never()).update(any());
    }

    @Test
    void shouldCompletePaidReadyOrderWithActiveItems() {
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderDomain order = completableOrder(restaurantId, orderId);
        when(orderPersistence.findByIdWithItems(orderId)).thenReturn(Optional.of(order));
        when(orderPersistence.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.completeOrder(restaurantId, orderId, UUID.randomUUID());

        verify(orderPersistence).update(any());
    }

    @Test
    void shouldRejectCancelingMoreThanActiveQuantity() {
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        OrderItemDomain item = OrderItemDomain.builder()
                .id(orderItemId)
                .quantity(new BigDecimal("3"))
                .canceledQuantity(new BigDecimal("2"))
                .unitPriceSnapshot(new BigDecimal("10000"))
                .build();
        OrderDomain order = OrderDomain.builder()
                .id(orderId)
                .restaurantId(restaurantId)
                .orderStatus(OrderStatus.OPEN)
                .kitchenStatus(KitchenStatus.PENDING)
                .items(List.of(item))
                .build();
        when(orderPersistence.findByIdWithItems(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> useCase.cancelOrderItem(
                restaurantId,
                orderId,
                orderItemId,
                new BigDecimal("2"),
                "Customer request",
                null,
                UUID.randomUUID()));

        verify(orderPersistence, never()).applyItemCancellation(any(), any(), any(), any());
    }

    @Test
    void shouldRejectPaymentForExpiredAwaitingOrder() {
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderDomain order = OrderDomain.builder()
                .id(orderId)
                .restaurantId(restaurantId)
                .businessDayId(businessDayId)
                .orderStatus(OrderStatus.AWAITING_PAYMENT)
                .paymentExpiredAt(OffsetDateTime.now())
                .totalGrossSnapshot(new BigDecimal("10000"))
                .build();
        when(orderPersistence.findByIdWithItems(orderId)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> useCase.registerPayment(
                restaurantId,
                orderId,
                PaymentDomain.builder().cashSessionId(cashSessionId).amount(new BigDecimal("1000")).build(),
                UUID.randomUUID(),
                UUID.randomUUID()));

        verify(orderPersistence, never()).savePayment(any());
    }

    @Test
    void shouldRejectPaymentAboveRemainingBalance() {
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderDomain order = OrderDomain.builder()
                .id(orderId)
                .restaurantId(restaurantId)
                .businessDayId(businessDayId)
                .orderStatus(OrderStatus.AWAITING_PAYMENT)
                .totalGrossSnapshot(new BigDecimal("10000"))
                .build();
        when(orderPersistence.findByIdWithItems(orderId)).thenReturn(Optional.of(order));
        when(orderPersistence.sumRecordedPayments(orderId)).thenReturn(new BigDecimal("4000"));
        when(orderPersistence.sumRecordedRefunds(orderId)).thenReturn(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> useCase.registerPayment(
                restaurantId,
                orderId,
                PaymentDomain.builder().cashSessionId(cashSessionId).amount(new BigDecimal("7000")).build(),
                UUID.randomUUID(),
                UUID.randomUUID()));

        verify(orderPersistence, never()).savePayment(any());
    }

    @Test
    void shouldExpirePartiallyPaidOrderAndReleaseReservations() {
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        OrderDomain order = OrderDomain.builder()
                .id(orderId)
                .restaurantId(restaurantId)
                .createdBy(actorId)
                .orderStatus(OrderStatus.AWAITING_PAYMENT)
                .totalGrossSnapshot(new BigDecimal("10000"))
                .items(List.of())
                .build();
        when(orderPersistence.lockExpiredAwaitingPayments(eq(now), anyInt())).thenReturn(List.of(order));
        when(orderPersistence.findByIdWithItems(orderId)).thenReturn(Optional.of(order));
        when(orderPersistence.sumRecordedPayments(orderId)).thenReturn(new BigDecimal("4000"));
        when(orderPersistence.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.processExpiredAwaitingPayments(now, 100);

        verify(orderPersistence).releaseReservationsByOrder(orderId, actorId);
        verify(orderPersistence).update(any());
    }

    private OrderDomain completableOrder(UUID restaurantId, UUID orderId) {
        return OrderDomain.builder()
                .id(orderId)
                .restaurantId(restaurantId)
                .orderStatus(OrderStatus.OPEN)
                .paymentStatus(PaymentStatus.PAID)
                .kitchenStatus(KitchenStatus.READY)
                .refundDueSnapshot(BigDecimal.ZERO)
                .items(List.of(OrderItemDomain.builder()
                        .id(UUID.randomUUID())
                        .quantity(BigDecimal.ONE)
                        .canceledQuantity(BigDecimal.ZERO)
                        .build()))
                .build();
    }
}
