package com.beet.backend.modules.order.application.handler;

import com.beet.backend.modules.cash.domain.api.CashSessionQueryPort;
import com.beet.backend.modules.order.application.dto.OrderCreateRequest;
import com.beet.backend.modules.order.domain.api.OrderBillServicePort;
import com.beet.backend.modules.order.domain.api.OrderServicePort;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.modules.user.domain.model.User;
import com.beet.backend.shared.infrastructure.security.CustomUserDetails;
import com.beet.backend.shared.infrastructure.security.DeviceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderHandlerImplTest {

    @Mock
    private OrderServicePort orderService;

    @Mock
    private OrderBillServicePort orderBillService;

    @Mock
    private CashSessionQueryPort cashSessionQuery;

    @Mock
    private DeviceContext deviceContext;

    @InjectMocks
    private OrderHandlerImpl handler;

    private UUID userId;
    private UUID restaurantId;
    private UUID deviceId;

    @BeforeEach
    void setUpAuthentication() {
        userId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        deviceId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("cashier@example.com")
                .passwordHash("hash")
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCaptureOriginDeviceWhenCreatingDraftOrder() {
        OrderCreateRequest request = takeoutRequest();

        when(deviceContext.getDeviceId()).thenReturn(deviceId);
        when(orderService.createDraft(any(OrderDomain.class), eq(userId), eq(deviceId)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        handler.create(restaurantId, request);

        ArgumentCaptor<OrderDomain> orderCaptor = ArgumentCaptor.forClass(OrderDomain.class);
        verify(orderService).createDraft(orderCaptor.capture(), eq(userId), eq(deviceId));
        assertEquals(ServiceType.TAKEOUT, orderCaptor.getValue().getServiceType());
        verify(cashSessionQuery, never()).getActiveSession(any(), any());
    }

    @Test
    void shouldNotRequireCashSessionWhenCreatingOrder() {
        OrderCreateRequest request = takeoutRequest();

        when(deviceContext.getDeviceId()).thenReturn(deviceId);
        when(orderService.createDraft(any(OrderDomain.class), eq(userId), eq(deviceId)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        handler.create(restaurantId, request);

        verify(orderService).createDraft(any(OrderDomain.class), eq(userId), eq(deviceId));
        verify(cashSessionQuery, never()).getActiveSession(any(), any());
        verify(orderService, never()).createOrder(any(), any());
    }

    private OrderCreateRequest takeoutRequest() {
        return new OrderCreateRequest(
                ServiceType.TAKEOUT,
                null,
                "Customer",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of());
    }
}
