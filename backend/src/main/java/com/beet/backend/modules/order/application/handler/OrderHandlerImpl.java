package com.beet.backend.modules.order.application.handler;

import com.beet.backend.modules.cash.domain.api.CashSessionQueryPort;
import com.beet.backend.modules.cash.domain.exception.CashSessionNotFoundException;
import com.beet.backend.modules.cash.domain.exception.CashSessionRequiredException;
import com.beet.backend.modules.cash.domain.model.CashSessionDomain;
import com.beet.backend.modules.order.application.dto.AddOrderItemRequest;
import com.beet.backend.modules.order.application.dto.CancelOrderRequest;
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
import com.beet.backend.modules.order.application.dto.PosCatalogResponse;
import com.beet.backend.modules.order.application.dto.SplitBillsRequest;
import com.beet.backend.modules.order.domain.api.OrderBillServicePort;
import com.beet.backend.modules.order.domain.api.OrderServicePort;
import com.beet.backend.modules.order.domain.exception.OrderNotFoundException;
import com.beet.backend.modules.order.domain.model.BillPaymentStatus;
import com.beet.backend.modules.order.domain.model.BillSearchCriteria;
import com.beet.backend.modules.order.domain.model.DeliveryStatus;
import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.KitchenTicketDomain;
import com.beet.backend.modules.order.domain.model.KitchenTicketLineDomain;
import com.beet.backend.modules.order.domain.model.KitchenTicketStatus;
import com.beet.backend.modules.order.domain.model.OrderBillAllocationDomain;
import com.beet.backend.modules.order.domain.model.OrderBillDomain;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTaxDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTemplateOptionDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTemplateSlotDomain;
import com.beet.backend.modules.order.domain.model.OrderLineType;
import com.beet.backend.modules.order.domain.model.OrderSearchCriteria;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.OrderTaxDomain;
import com.beet.backend.modules.order.domain.model.PaymentDomain;
import com.beet.backend.modules.order.domain.model.PaymentMethodDomain;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.PosCatalogEntryDomain;
import com.beet.backend.modules.order.domain.model.PosTemplateOptionDomain;
import com.beet.backend.modules.order.domain.model.PosTemplateSlotDomain;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.shared.infrastructure.input.rest.ApiGenericResponse;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import com.beet.backend.shared.infrastructure.security.DeviceContext;
import com.beet.backend.shared.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderHandlerImpl implements OrderHandler {

    private final OrderServicePort orderService;
    private final OrderBillServicePort orderBillService;
    private final CashSessionQueryPort cashSessionQuery;
    private final DeviceContext deviceContext;

    @Override
    public ApiGenericResponse<PageResponse<PosCatalogResponse>> posCatalog(UUID restaurantId, int page, int size,
            String search, UUID menuId, UUID submenuId, String availability, String referenceType, String sort) {
        PageResponse<PosCatalogEntryDomain> result = orderService.findPosCatalog(
                restaurantId, page, size, search, menuId, submenuId, availability, referenceType, sort);
        List<PosCatalogResponse> content = result.content().stream().map(this::toPosCatalogResponse).toList();
        return ApiGenericResponse.success(PageResponse.of(content, result.totalElements(), result.number(), result.size()));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> createDraft(UUID restaurantId, OrderCreateRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        UUID deviceId = deviceContext.getDeviceId();
        OrderDomain created = orderService.createDraft(toOrderDomain(restaurantId, request), userId, deviceId);
        return ApiGenericResponse.success(toDetailResponse(created));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> create(UUID restaurantId, OrderCreateRequest request) {
        return createDraft(restaurantId, request);
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> confirm(UUID restaurantId, UUID orderId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        OrderDomain confirmed = orderService.confirmOrder(restaurantId, orderId, userId);
        return ApiGenericResponse.success(toDetailResponse(confirmed));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> complete(UUID restaurantId, UUID orderId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        OrderDomain completed = orderService.completeOrder(restaurantId, orderId, userId);
        return ApiGenericResponse.success(toDetailResponse(completed));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> cancel(UUID restaurantId, UUID orderId, CancelOrderRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        OrderDomain canceled = orderService.cancelOrder(
                restaurantId, orderId, request != null ? request.reason() : null, userId);
        return ApiGenericResponse.success(toDetailResponse(canceled));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> addItem(UUID restaurantId, UUID orderId,
            AddOrderItemRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        OrderDomain updated = orderService.addItem(restaurantId, orderId, toOrderItemDomain(request), userId);
        return ApiGenericResponse.success(toDetailResponse(updated));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> updateItemQuantity(UUID restaurantId, UUID orderId,
            UUID orderItemId, OrderItemQuantityRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        OrderDomain updated = orderService.updateItemQuantity(
                restaurantId, orderId, orderItemId, request.quantity(), userId);
        return ApiGenericResponse.success(toDetailResponse(updated));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> removeItem(UUID restaurantId, UUID orderId, UUID orderItemId) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        OrderDomain updated = orderService.removeItem(restaurantId, orderId, orderItemId, userId);
        return ApiGenericResponse.success(toDetailResponse(updated));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse> getById(UUID restaurantId, UUID orderId) {
        OrderDomain found = orderService.findById(restaurantId, orderId)
                .orElseThrow(() -> OrderNotFoundException.forId(orderId));
        return ApiGenericResponse.success(toDetailResponse(found));
    }

    @Override
    public ApiGenericResponse<PageResponse<OrderResponse>> list(UUID restaurantId, int page, int size, String sort,
            OrderStatus orderStatus, PaymentStatus paymentStatus, KitchenStatus kitchenStatus, ServiceType serviceType,
            UUID tableId, OffsetDateTime dateFrom, OffsetDateTime dateTo, UUID cashSessionId, UUID cashRegisterId,
            UUID createdBy, String customer, UUID paymentMethodId, BigDecimal minTotal, BigDecimal maxTotal,
            DeliveryStatus deliveryStatus, String search) {
        OrderSearchCriteria criteria = new OrderSearchCriteria(restaurantId, page, size, sort, orderStatus,
                paymentStatus, kitchenStatus, serviceType, tableId, dateFrom, dateTo, cashSessionId, cashRegisterId,
                createdBy, customer, paymentMethodId, minTotal, maxTotal, deliveryStatus, search);
        PageResponse<OrderDomain> result = orderService.findAllPaged(criteria);
        List<OrderResponse> content = result.content().stream().map(this::toResponse).toList();
        return ApiGenericResponse.success(PageResponse.of(content, result.totalElements(), result.number(), result.size()));
    }

    @Override
    public ApiGenericResponse<PageResponse<OrderDetailResponse.KitchenTicketResponse>> listKitchenTickets(
            UUID restaurantId, KitchenTicketStatus status, int page, int size) {
        PageResponse<KitchenTicketDomain> result = orderService.listKitchenTickets(restaurantId, status, page, size);
        List<OrderDetailResponse.KitchenTicketResponse> content = result.content().stream()
                .map(this::toKitchenTicketResponse)
                .toList();
        return ApiGenericResponse.success(PageResponse.of(content, result.totalElements(), result.number(), result.size()));
    }

    @Override
    public ApiGenericResponse<OrderDetailResponse.KitchenTicketResponse> updateKitchenTicket(UUID restaurantId,
            UUID ticketId, KitchenTicketStatusRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        KitchenTicketDomain updated = orderService.updateKitchenTicketStatus(restaurantId, ticketId, request.status(), userId);
        return ApiGenericResponse.success(toKitchenTicketResponse(updated));
    }

    @Override
    public ApiGenericResponse<List<PaymentMethodResponse>> listPaymentMethods(UUID restaurantId) {
        return ApiGenericResponse.success(orderService.listPaymentMethods(restaurantId).stream()
                .map(this::toPaymentMethodResponse)
                .toList());
    }

    @Override
    public ApiGenericResponse<PaymentMethodResponse> createPaymentMethod(UUID restaurantId, PaymentMethodRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        PaymentMethodDomain created = orderService.createPaymentMethod(PaymentMethodDomain.builder()
                .restaurantId(restaurantId)
                .code(request.code())
                .name(request.name())
                .type(request.type())
                .isActive(request.isActive() == null || request.isActive())
                .requiresReference(Boolean.TRUE.equals(request.requiresReference()))
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .build(), userId);
        return ApiGenericResponse.success(toPaymentMethodResponse(created));
    }

    @Override
    public ApiGenericResponse<PaymentMethodResponse> updatePaymentMethod(UUID restaurantId, UUID methodId,
            PaymentMethodRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        PaymentMethodDomain updated = orderService.updatePaymentMethod(
                restaurantId, methodId, request.isActive(), request.name(), request.requiresReference(),
                request.sortOrder(), userId);
        return ApiGenericResponse.success(toPaymentMethodResponse(updated));
    }

    @Override
    public ApiGenericResponse<PaymentResponse> registerPayment(UUID restaurantId, UUID orderId, PaymentRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        UUID deviceId = deviceContext.getDeviceId();
        CashSessionDomain cashSession;
        try {
            cashSession = cashSessionQuery.getActiveSession(restaurantId, deviceId);
        } catch (CashSessionNotFoundException exception) {
            throw CashSessionRequiredException.forDevice(deviceId);
        }
        PaymentDomain payment = PaymentDomain.builder()
                .restaurantId(restaurantId)
                .orderId(orderId)
                .orderBillId(request.orderBillId())
                .paymentMethodId(request.paymentMethodId())
                .cashSessionId(cashSession.getId())
                .deviceId(deviceId)
                .amount(request.amount())
                .tipAmount(request.tipAmount())
                .externalReference(request.externalReference())
                .notes(request.notes())
                .build();
        return ApiGenericResponse.success(toPaymentResponse(
                orderService.registerPayment(restaurantId, orderId, payment, userId, deviceId)));
    }

    @Override
    public ApiGenericResponse<List<OrderBillResponse>> splitBills(UUID restaurantId, UUID orderId,
            SplitBillsRequest request) {
        UUID userId = SecurityUtils.getAuthenticatedUserId();
        List<OrderBillDomain> bills = orderBillService.split(restaurantId, orderId, request.mode(),
                request.bills().stream()
                        .map(bill -> new OrderBillServicePort.SplitBillCommand(
                                bill.label(), bill.amount(), bill.percentage(),
                                bill.items() == null ? List.of() : bill.items().stream()
                                        .map(item -> new OrderBillServicePort.SplitBillItemCommand(
                                                item.orderItemId(), item.quantity()))
                                        .toList()))
                        .toList(),
                userId);
        return ApiGenericResponse.success(bills.stream().map(this::toBillResponse).toList());
    }

    @Override
    public ApiGenericResponse<PageResponse<OrderBillResponse>> listBills(UUID restaurantId, UUID orderId, int page,
            int size, BillPaymentStatus paymentStatus, String search, String sort) {
        PageResponse<OrderBillDomain> result = orderBillService.list(
                new BillSearchCriteria(restaurantId, orderId, page, size, sort, paymentStatus, search));
        List<OrderBillResponse> content = result.content().stream().map(this::toBillResponse).toList();
        return ApiGenericResponse.success(PageResponse.of(content, result.totalElements(), result.number(), result.size()));
    }

    private OrderDomain toOrderDomain(UUID restaurantId, OrderCreateRequest request) {
        return OrderDomain.builder()
                .restaurantId(restaurantId)
                .serviceType(request.serviceType())
                .tableId(request.tableId())
                .customerName(request.customerName())
                .customerPhone(request.customerPhone())
                .deliveryContactName(request.deliveryContactName())
                .deliveryPhone(request.deliveryPhone())
                .deliveryAddress(request.deliveryAddress())
                .deliveryNotes(request.deliveryNotes())
                .deliveryFee(request.deliveryFee())
                .notes(request.notes())
                .items(toOrderItemDomains(request.items()))
                .build();
    }

    private List<OrderItemDomain> toOrderItemDomains(List<OrderCreateRequest.OrderItemRequest> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().map(this::toOrderItemDomain).toList();
    }

    private OrderItemDomain toOrderItemDomain(AddOrderItemRequest request) {
        return OrderItemDomain.builder()
                .lineType(parseLineType(request.lineType()))
                .itemId(request.itemId())
                .templateId(request.templateId())
                .submenuNodeId(request.submenuNodeId())
                .quantity(request.quantity())
                .unitPriceSnapshot(request.unitPrice())
                .notes(request.notes())
                .templateSlots(toTemplateSlots(request.slots()))
                .build();
    }

    private OrderItemDomain toOrderItemDomain(OrderCreateRequest.OrderItemRequest item) {
        return OrderItemDomain.builder()
                .lineType(parseLineType(item.lineType()))
                .itemId(item.itemId())
                .templateId(item.templateId())
                .submenuNodeId(item.submenuNodeId())
                .quantity(item.quantity())
                .unitPriceSnapshot(item.unitPrice())
                .notes(item.notes())
                .templateSlots(toTemplateSlots(item.slots()))
                .build();
    }

    private List<OrderItemTemplateSlotDomain> toTemplateSlots(
            List<OrderCreateRequest.TemplateSlotSelectionRequest> slots) {
        if (slots == null) {
            return List.of();
        }
        return slots.stream()
                .map(slot -> OrderItemTemplateSlotDomain.builder()
                        .templateSlotId(slot.slotId())
                        .options(slot.options() == null ? List.of() : slot.options().stream()
                                .map(option -> OrderItemTemplateOptionDomain.builder()
                                        .slotOptionId(option.slotOptionId())
                                        .itemId(option.itemId())
                                        .quantity(option.quantity())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    private OrderLineType parseLineType(String value) {
        return value == null || value.isBlank() ? OrderLineType.PRODUCT : OrderLineType.valueOf(value);
    }

    private OrderResponse toResponse(OrderDomain order) {
        return new OrderResponse(
                order.getId(),
                order.getRestaurantId(),
                order.getOriginCashSessionId() != null ? order.getOriginCashSessionId() : order.getCashSessionId(),
                order.getBusinessDate(),
                order.getDailySequence(),
                order.getOrderNumber(),
                order.getPublicCode(),
                order.getDisplayCode(),
                order.getOrderStatus(),
                order.getKitchenStatus(),
                order.getPaymentStatus(),
                order.getServiceType(),
                order.getTableId(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getDeliveryStatus(),
                order.getSubtotalGrossSnapshot(),
                order.getTaxAmountSnapshot(),
                order.getTotalGrossSnapshot(),
                order.getTipTotalSnapshot(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    private OrderDetailResponse toDetailResponse(OrderDomain order) {
        return new OrderDetailResponse(
                order.getId(),
                order.getRestaurantId(),
                order.getCashSessionId(),
                order.getOriginCashSessionId(),
                order.getOriginDeviceId(),
                order.getBusinessDate(),
                order.getDailySequence(),
                order.getOrderNumber(),
                order.getPublicCode(),
                order.getDisplayCode(),
                order.getOrderStatus(),
                order.getKitchenStatus(),
                order.getPaymentStatus(),
                order.getServiceType(),
                order.getOperationModeSnapshot(),
                order.getTableId(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getDeliveryContactName(),
                order.getDeliveryPhone(),
                order.getDeliveryAddress(),
                order.getDeliveryNotes(),
                order.getDeliveryFee(),
                order.getDeliveryStatus(),
                order.isPrepaymentRequiredSnapshot(),
                order.getTaxRateSnapshot(),
                order.getSubtotalGrossSnapshot(),
                order.getTaxAmountSnapshot(),
                order.getTotalGrossSnapshot(),
                order.getTipTotalSnapshot(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems() == null ? List.of() : order.getItems().stream().map(this::toItemResponse).toList(),
                order.getTaxes() == null ? List.of() : order.getTaxes().stream().map(this::toTaxResponse).toList(),
                order.getKitchenTickets() == null ? List.of()
                        : order.getKitchenTickets().stream().map(this::toKitchenTicketResponse).toList(),
                order.getPayments() == null ? List.of()
                        : order.getPayments().stream().map(this::toNestedPaymentResponse).toList());
    }

    private OrderDetailResponse.OrderItemResponse toItemResponse(OrderItemDomain item) {
        return new OrderDetailResponse.OrderItemResponse(
                item.getId(),
                item.getLineType(),
                item.getItemId(),
                item.getTemplateId(),
                item.getSubmenuNodeId(),
                item.getItemNameSnapshot(),
                item.getUnitPriceSnapshot(),
                item.getTheoreticalCostSnapshot(),
                item.getQuantity(),
                item.getSubtotalGrossSnapshot(),
                item.getNotes(),
                item.getTemplateSlots() == null ? List.of()
                        : item.getTemplateSlots().stream().map(this::toTemplateSlotResponse).toList(),
                item.getTaxes() == null ? List.of() : item.getTaxes().stream().map(this::toItemTaxResponse).toList());
    }

    private OrderDetailResponse.TemplateSlotSnapshotResponse toTemplateSlotResponse(OrderItemTemplateSlotDomain slot) {
        return new OrderDetailResponse.TemplateSlotSnapshotResponse(
                slot.getId(),
                slot.getTemplateSlotId(),
                slot.getSlotNameSnapshot(),
                slot.getMinSelectionSnapshot(),
                slot.getMaxSelectionSnapshot(),
                slot.getSortOrder(),
                slot.getOptions() == null ? List.of()
                        : slot.getOptions().stream().map(this::toTemplateOptionResponse).toList());
    }

    private OrderDetailResponse.TemplateOptionSnapshotResponse toTemplateOptionResponse(
            OrderItemTemplateOptionDomain option) {
        return new OrderDetailResponse.TemplateOptionSnapshotResponse(
                option.getId(),
                option.getSlotOptionId(),
                option.getItemId(),
                option.getItemNameSnapshot(),
                option.getQuantity(),
                option.getSurchargeSnapshot(),
                option.getTheoreticalCostSnapshot());
    }

    private OrderDetailResponse.OrderTaxResponse toTaxResponse(OrderTaxDomain tax) {
        return new OrderDetailResponse.OrderTaxResponse(
                tax.getId(), tax.getTaxId(), tax.getTaxNameSnapshot(), tax.getTaxRateSnapshot(),
                tax.getTaxBaseSnapshot(), tax.getTaxAmountSnapshot());
    }

    private OrderDetailResponse.OrderItemTaxResponse toItemTaxResponse(OrderItemTaxDomain tax) {
        return new OrderDetailResponse.OrderItemTaxResponse(
                tax.getId(), tax.getOrderItemId(), tax.getTaxId(), tax.getTaxNameSnapshot(),
                tax.getTaxRateSnapshot(), tax.getTaxBaseSnapshot(), tax.getTaxAmountSnapshot());
    }

    private OrderDetailResponse.KitchenTicketResponse toKitchenTicketResponse(KitchenTicketDomain ticket) {
        return new OrderDetailResponse.KitchenTicketResponse(
                ticket.getId(),
                ticket.getOrderId(),
                ticket.getOrderNumber(),
                ticket.getOrderPublicCode(),
                ticket.getOrderDisplayCode(),
                ticket.getStatus(),
                ticket.getSentAt(),
                ticket.getStartedAt(),
                ticket.getReadyAt(),
                ticket.getCanceledAt(),
                ticket.getLines() == null ? List.of()
                        : ticket.getLines().stream().map(this::toKitchenLineResponse).toList());
    }

    private OrderDetailResponse.KitchenTicketLineResponse toKitchenLineResponse(KitchenTicketLineDomain line) {
        return new OrderDetailResponse.KitchenTicketLineResponse(
                line.getId(), line.getOrderItemId(), line.getQuantity(), line.getItemNameSnapshot(), line.getNotes());
    }

    private OrderDetailResponse.PaymentResponse toNestedPaymentResponse(PaymentDomain payment) {
        return new OrderDetailResponse.PaymentResponse(
                payment.getId(),
                payment.getPaymentMethodId(),
                payment.getCashSessionId(),
                payment.getAmount(),
                payment.getTipAmount(),
                payment.getStatus(),
                payment.getExternalReference(),
                payment.getCreatedAt());
    }

    private PaymentResponse toPaymentResponse(PaymentDomain payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getRestaurantId(),
                payment.getOrderId(),
                payment.getOrderBillId(),
                payment.getPaymentMethodId(),
                payment.getCashSessionId(),
                payment.getAmount(),
                payment.getTipAmount(),
                payment.getStatus(),
                payment.getExternalReference(),
                payment.getNotes(),
                payment.getCreatedAt());
    }

    private PaymentMethodResponse toPaymentMethodResponse(PaymentMethodDomain method) {
        return new PaymentMethodResponse(
                method.getId(),
                method.getRestaurantId(),
                method.getCode(),
                method.getName(),
                method.getType(),
                method.isActive(),
                method.isRequiresReference(),
                method.getSortOrder(),
                method.getCreatedAt(),
                method.getUpdatedAt());
    }

    private OrderBillResponse toBillResponse(OrderBillDomain bill) {
        return new OrderBillResponse(
                bill.getId(),
                bill.getOrderId(),
                bill.getLabel(),
                bill.getSplitMode(),
                bill.getSubtotalGrossSnapshot(),
                bill.getTipTotalSnapshot(),
                bill.getTotalPaidSnapshot(),
                bill.getPaymentStatus(),
                bill.getCreatedAt(),
                bill.getUpdatedAt(),
                bill.getAllocations() == null ? List.of()
                        : bill.getAllocations().stream().map(this::toBillAllocationResponse).toList());
    }

    private OrderBillResponse.OrderBillAllocationResponse toBillAllocationResponse(OrderBillAllocationDomain allocation) {
        return new OrderBillResponse.OrderBillAllocationResponse(
                allocation.getId(), allocation.getOrderItemId(), allocation.getQuantity(), allocation.getAmount());
    }

    private PosCatalogResponse toPosCatalogResponse(PosCatalogEntryDomain entry) {
        return new PosCatalogResponse(
                entry.getNodeId(),
                entry.getMenuId(),
                entry.getMenuName(),
                entry.getSubmenuId(),
                entry.getSubmenuName(),
                entry.getReferenceType(),
                entry.getReferenceId(),
                entry.getName(),
                entry.getDescription(),
                entry.getPrice(),
                entry.isAvailable(),
                entry.isLowStock(),
                entry.getUnavailableReason(),
                entry.getSortOrder(),
                entry.getInsufficientIngredients(),
                entry.getSlots() == null ? List.of() : entry.getSlots().stream().map(this::toPosSlotResponse).toList());
    }

    private PosCatalogResponse.PosTemplateSlotResponse toPosSlotResponse(PosTemplateSlotDomain slot) {
        return new PosCatalogResponse.PosTemplateSlotResponse(
                slot.getSlotId(),
                slot.getName(),
                slot.getMinSelection(),
                slot.getMaxSelection(),
                slot.getSortOrder(),
                slot.getOptions() == null ? List.of()
                        : slot.getOptions().stream().map(this::toPosOptionResponse).toList());
    }

    private PosCatalogResponse.PosTemplateOptionResponse toPosOptionResponse(PosTemplateOptionDomain option) {
        return new PosCatalogResponse.PosTemplateOptionResponse(
                option.getSlotOptionId(),
                option.getItemId(),
                option.getItemName(),
                option.getSurcharge(),
                option.getMaxQuantity(),
                option.isDefault(),
                option.isAvailable(),
                option.isLowStock(),
                option.getUnavailableReason(),
                option.getSortOrder(),
                option.getInsufficientIngredients());
    }
}
