package com.beet.backend.modules.order.domain.spi;

import com.beet.backend.modules.order.domain.model.InventoryReservationDomain;
import com.beet.backend.modules.order.domain.model.IngredientStockAvailabilityDomain;
import com.beet.backend.modules.order.domain.model.KitchenTicketDomain;
import com.beet.backend.modules.order.domain.model.KitchenTicketStatus;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.model.OrderItemCancellationDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTaxDomain;
import com.beet.backend.modules.order.domain.model.OrderItemIngredientRequirementDomain;
import com.beet.backend.modules.order.domain.model.OrderSearchCriteria;
import com.beet.backend.modules.order.domain.model.OrderTaxDomain;
import com.beet.backend.modules.order.domain.model.PaymentDomain;
import com.beet.backend.modules.order.domain.model.PaymentMethodDomain;
import com.beet.backend.modules.order.domain.model.PaymentRefundDomain;
import com.beet.backend.modules.order.domain.model.PosCatalogEntryDomain;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderPersistencePort {

    OrderDomain save(OrderDomain order);

    OrderDomain update(OrderDomain order);

    int nextDailySequence(UUID restaurantId, LocalDate businessDate);

    boolean existsPublicCode(UUID restaurantId, String publicCode);

    OrderItemDomain saveItem(OrderItemDomain item);

    void saveTemplateSnapshots(OrderItemDomain item);

    void saveIngredientRequirements(OrderItemDomain item);

    List<OrderItemIngredientRequirementDomain> findIngredientRequirements(UUID orderItemId);

    Optional<IngredientStockAvailabilityDomain> findIngredientStockAvailability(
            UUID restaurantId, UUID masterIngredientId);

    void updateItemQuantity(UUID orderItemId, BigDecimal quantity, BigDecimal subtotalGrossSnapshot, UUID updatedBy);

    void deleteItem(UUID orderItemId);

    OrderItemCancellationDomain applyItemCancellation(
            OrderItemCancellationDomain cancellation,
            BigDecimal originalQuantity,
            BigDecimal activeSubtotal,
            UUID userId);

    void cancelEmptyKitchenTickets(UUID orderId, UUID userId);

    void replaceOrderTaxes(UUID orderId, List<OrderTaxDomain> taxes);

    void replaceOrderItemTaxes(UUID orderId, List<OrderItemTaxDomain> taxes);

    Optional<OrderDomain> findByIdWithItems(UUID orderId);

    void lockOrder(UUID restaurantId, UUID orderId);

    PageResponse<OrderDomain> findAllPaged(OrderSearchCriteria criteria);

    PageResponse<OrderDomain> findAllPaged(UUID restaurantId, int page, int size, String search);

    PageResponse<PosCatalogEntryDomain> findPosCatalog(UUID restaurantId, int page, int size, String search,
            UUID menuId, UUID submenuId, String referenceType, String sort);

    KitchenTicketDomain saveKitchenTicket(KitchenTicketDomain ticket);

    Optional<KitchenTicketDomain> findKitchenTicket(UUID restaurantId, UUID ticketId);

    PageResponse<KitchenTicketDomain> findKitchenTickets(UUID restaurantId, KitchenTicketStatus status, int page,
            int size);

    KitchenTicketDomain updateKitchenTicketStatus(UUID restaurantId, UUID ticketId, KitchenTicketStatus status,
            UUID userId);

    List<InventoryReservationDomain> findActiveReservationsByOrderItem(UUID orderItemId);

    void saveReservations(List<InventoryReservationDomain> reservations);

    void releaseReservationsByOrderItem(UUID orderItemId, UUID userId);

    void releaseReservationsByOrder(UUID orderId, UUID userId);

    List<OrderDomain> lockExpiredAwaitingPayments(OffsetDateTime now, int limit);

    void consumeReservationsByTicket(UUID restaurantId, UUID ticketId, UUID userId);

    List<PaymentMethodDomain> findPaymentMethods(UUID restaurantId);

    PaymentMethodDomain savePaymentMethod(PaymentMethodDomain method);

    boolean existsPaymentMethodCode(UUID restaurantId, String code);

    int countActivePaymentMethods(UUID restaurantId);

    PaymentMethodDomain updatePaymentMethod(PaymentMethodDomain method);

    Optional<PaymentMethodDomain> findPaymentMethod(UUID restaurantId, UUID methodId);

    PaymentDomain savePayment(PaymentDomain payment);

    Optional<PaymentDomain> findPayment(UUID restaurantId, UUID orderId, UUID paymentId);

    BigDecimal sumRecordedPayments(UUID orderId);

    BigDecimal sumRecordedTips(UUID orderId);

    PaymentRefundDomain saveRefund(PaymentRefundDomain refund);

    List<PaymentRefundDomain> findRefundsByOrder(UUID restaurantId, UUID orderId);

    BigDecimal sumRecordedRefunds(UUID orderId);

    BigDecimal sumRecordedRefundsByPayment(UUID paymentId);
}
