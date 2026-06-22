package com.beet.backend.modules.order.application.handler;

import com.beet.backend.modules.order.application.dto.AddOrderItemRequest;
import com.beet.backend.modules.order.application.dto.CancelOrderRequest;
import com.beet.backend.modules.order.application.dto.CancelOrderItemRequest;
import com.beet.backend.modules.order.application.dto.KitchenTicketStatusRequest;
import com.beet.backend.modules.order.application.dto.OrderBillResponse;
import com.beet.backend.modules.order.application.dto.OrderCreateRequest;
import com.beet.backend.modules.order.application.dto.OrderDetailResponse;
import com.beet.backend.modules.order.application.dto.OrderItemQuantityRequest;
import com.beet.backend.modules.order.application.dto.OrderResponse;
import com.beet.backend.modules.order.application.dto.PaymentMethodRequest;
import com.beet.backend.modules.order.application.dto.PaymentMethodResponse;
import com.beet.backend.modules.order.application.dto.PaymentRequest;
import com.beet.backend.modules.order.application.dto.PaymentResponse;
import com.beet.backend.modules.order.application.dto.PaymentRefundRequest;
import com.beet.backend.modules.order.application.dto.PaymentRefundResponse;
import com.beet.backend.modules.order.application.dto.PosCatalogResponse;
import com.beet.backend.modules.order.application.dto.SplitBillsRequest;
import com.beet.backend.modules.order.domain.model.BillPaymentStatus;
import com.beet.backend.modules.order.domain.model.DeliveryStatus;
import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.KitchenTicketStatus;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.PaymentPendingState;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderHandler {

    ApiGenericResponse<PageResponse<PosCatalogResponse>> posCatalog(UUID restaurantId, int page, int size,
            String search, UUID menuId, UUID submenuId, String referenceType, String sort);

    ApiGenericResponse<OrderDetailResponse> createDraft(UUID restaurantId, OrderCreateRequest request);

    ApiGenericResponse<OrderDetailResponse> create(UUID restaurantId, OrderCreateRequest request);

    ApiGenericResponse<OrderDetailResponse> confirm(UUID restaurantId, UUID orderId);

    ApiGenericResponse<OrderDetailResponse> complete(UUID restaurantId, UUID orderId);

    ApiGenericResponse<OrderDetailResponse> cancel(UUID restaurantId, UUID orderId, CancelOrderRequest request);

    ApiGenericResponse<OrderDetailResponse> addItem(UUID restaurantId, UUID orderId, AddOrderItemRequest request);

    ApiGenericResponse<OrderDetailResponse> addItems(UUID restaurantId, UUID orderId,
            com.beet.backend.modules.order.application.dto.AddOrderItemsBatchRequest request);

    ApiGenericResponse<OrderDetailResponse> reactivatePayment(UUID restaurantId, UUID orderId);

    ApiGenericResponse<OrderDetailResponse> updateItemQuantity(UUID restaurantId, UUID orderId,
            UUID orderItemId, OrderItemQuantityRequest request);

    ApiGenericResponse<OrderDetailResponse> removeItem(UUID restaurantId, UUID orderId, UUID orderItemId);

    ApiGenericResponse<OrderDetailResponse> cancelItem(UUID restaurantId, UUID orderId, UUID orderItemId,
            CancelOrderItemRequest request);

    ApiGenericResponse<OrderDetailResponse> getById(UUID restaurantId, UUID orderId);

    ApiGenericResponse<PageResponse<OrderResponse>> list(UUID restaurantId, int page, int size, String sort,
            OrderStatus orderStatus, PaymentStatus paymentStatus, KitchenStatus kitchenStatus, ServiceType serviceType,
            UUID tableId, OffsetDateTime dateFrom, OffsetDateTime dateTo, UUID cashSessionId, UUID cashRegisterId,
            UUID createdBy, String customer, UUID paymentMethodId, BigDecimal minTotal, BigDecimal maxTotal,
            DeliveryStatus deliveryStatus, PaymentPendingState paymentPendingState, String search);

    ApiGenericResponse<PageResponse<OrderDetailResponse.KitchenTicketResponse>> listKitchenTickets(UUID restaurantId,
            KitchenTicketStatus status, int page, int size);

    ApiGenericResponse<OrderDetailResponse.KitchenTicketResponse> updateKitchenTicket(UUID restaurantId,
            UUID ticketId, KitchenTicketStatusRequest request);

    ApiGenericResponse<List<PaymentMethodResponse>> listPaymentMethods(UUID restaurantId);

    ApiGenericResponse<PaymentMethodResponse> createPaymentMethod(UUID restaurantId, PaymentMethodRequest request);

    ApiGenericResponse<PaymentMethodResponse> updatePaymentMethod(UUID restaurantId, UUID methodId,
            PaymentMethodRequest request);

    ApiGenericResponse<PaymentResponse> registerPayment(UUID restaurantId, UUID orderId, PaymentRequest request);

    ApiGenericResponse<PaymentRefundResponse> registerRefund(UUID restaurantId, UUID orderId,
            PaymentRefundRequest request);

    ApiGenericResponse<List<PaymentRefundResponse>> listRefunds(UUID restaurantId, UUID orderId);

    ApiGenericResponse<List<OrderBillResponse>> splitBills(UUID restaurantId, UUID orderId, SplitBillsRequest request);

    ApiGenericResponse<PageResponse<OrderBillResponse>> listBills(UUID restaurantId, UUID orderId, int page, int size,
            BillPaymentStatus paymentStatus, String search, String sort);
}
