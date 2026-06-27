package com.beet.backend.modules.order.domain.api;

import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.model.OrderItemInventoryDisposition;
import com.beet.backend.modules.order.domain.model.PaymentRefundDomain;
import com.beet.backend.modules.order.domain.model.OrderSearchCriteria;
import com.beet.backend.modules.order.domain.model.KitchenTicketDomain;
import com.beet.backend.modules.order.domain.model.KitchenTicketStatus;
import com.beet.backend.modules.order.domain.model.PaymentDomain;
import com.beet.backend.modules.order.domain.model.PaymentMethodDomain;
import com.beet.backend.modules.order.domain.model.PosCatalogEntryDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderServicePort {

    OrderDomain createDraft(OrderDomain order, UUID userId, UUID deviceId);

    OrderDomain confirmOrder(UUID restaurantId, UUID orderId, UUID userId);

    OrderDomain completeOrder(UUID restaurantId, UUID orderId, UUID userId);

    OrderDomain cancelOrder(UUID restaurantId, UUID orderId, String reason,
            List<OrderCancellationDecision> decisions, UUID userId);

    OrderDomain createOrder(OrderDomain order, UUID userId);

    OrderDomain addItem(UUID restaurantId, UUID orderId, OrderItemDomain item, UUID userId);

    OrderDomain addItems(UUID restaurantId, UUID orderId, List<OrderItemDomain> items, UUID userId);

    OrderDomain reactivatePayment(UUID restaurantId, UUID orderId, UUID userId);

    int processExpiredAwaitingPayments(OffsetDateTime now, int batchSize);

    OrderDomain updateItemQuantity(UUID restaurantId, UUID orderId, UUID orderItemId, BigDecimal quantity, UUID userId);

    OrderDomain removeItem(UUID restaurantId, UUID orderId, UUID orderItemId, UUID userId);

    OrderDomain cancelOrderItem(UUID restaurantId, UUID orderId, UUID orderItemId, BigDecimal quantity,
            String reason, OrderItemInventoryDisposition inventoryDisposition, UUID userId);

    Optional<OrderDomain> findById(UUID restaurantId, UUID orderId);

    PageResponse<OrderDomain> findAllPaged(OrderSearchCriteria criteria);

    PageResponse<OrderDomain> findAllPaged(UUID restaurantId, int page, int size, String search);

    PageResponse<PosCatalogEntryDomain> findPosCatalog(UUID restaurantId, int page, int size, String search,
            UUID menuId, UUID submenuId, String referenceType, String sort);

    KitchenTicketDomain updateKitchenTicketStatus(UUID restaurantId, UUID ticketId, KitchenTicketStatus status,
            UUID userId);

    PageResponse<KitchenTicketDomain> listKitchenTickets(UUID restaurantId, KitchenTicketStatus status, int page,
            int size);

    List<PaymentMethodDomain> listPaymentMethods(UUID restaurantId);

    PaymentMethodDomain createPaymentMethod(PaymentMethodDomain method, UUID userId);

    void ensureDefaultPaymentMethods(UUID restaurantId, UUID userId);

    PaymentMethodDomain updatePaymentMethod(UUID restaurantId, UUID methodId, Boolean isActive,
            String name, Boolean requiresReference, Integer sortOrder, UUID userId);

    PaymentDomain registerPayment(UUID restaurantId, UUID orderId, PaymentDomain payment, UUID userId, UUID deviceId);

    PaymentRefundDomain registerRefund(UUID restaurantId, UUID orderId, PaymentRefundDomain refund, UUID userId,
            UUID deviceId);

    List<PaymentRefundDomain> listRefunds(UUID restaurantId, UUID orderId);

    record OrderCancellationDecision(
            UUID orderItemId,
            OrderItemInventoryDisposition inventoryDisposition) {
    }
}
