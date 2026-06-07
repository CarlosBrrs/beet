package com.beet.backend.modules.order.domain.usecase;

import com.beet.backend.modules.item.domain.exception.ItemNotFoundException;
import com.beet.backend.modules.item.domain.api.RecipeCalculationServicePort;
import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.model.RecipeCalculationResult;
import com.beet.backend.modules.item.domain.model.RecipeIngredientRequirement;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import com.beet.backend.modules.menu.domain.model.SubmenuNodeDomain;
import com.beet.backend.modules.menu.domain.model.SubmenuNodeType;
import com.beet.backend.modules.menu.domain.spi.SubmenuNodeQueryPort;
import com.beet.backend.modules.order.domain.api.OrderServicePort;
import com.beet.backend.modules.order.domain.exception.OrderItemNotFoundException;
import com.beet.backend.modules.order.domain.exception.OrderNotFoundException;
import com.beet.backend.modules.order.domain.model.DeliveryStatus;
import com.beet.backend.modules.order.domain.model.InventoryReservationDomain;
import com.beet.backend.modules.order.domain.model.InventoryReservationStatus;
import com.beet.backend.modules.order.domain.model.IngredientStockAvailabilityDomain;
import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.KitchenTicketDomain;
import com.beet.backend.modules.order.domain.model.KitchenTicketLineDomain;
import com.beet.backend.modules.order.domain.model.KitchenTicketStatus;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.model.OrderItemIngredientRequirementDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTaxDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTemplateOptionDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTemplateSlotDomain;
import com.beet.backend.modules.order.domain.model.OrderLineType;
import com.beet.backend.modules.order.domain.model.OrderSearchCriteria;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.OrderTaxDefinition;
import com.beet.backend.modules.order.domain.model.OrderTaxDomain;
import com.beet.backend.modules.order.domain.model.PaymentDomain;
import com.beet.backend.modules.order.domain.model.PaymentMethodDomain;
import com.beet.backend.modules.order.domain.model.PaymentRecordStatus;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.PosCatalogEntryDomain;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.modules.order.domain.spi.OrderPersistencePort;
import com.beet.backend.modules.order.domain.spi.OrderTableGateway;
import com.beet.backend.modules.order.domain.spi.OrderTaxQueryPort;
import com.beet.backend.modules.restaurant.domain.exception.RestaurantNotFoundException;
import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPersistencePort;
import com.beet.backend.modules.template.domain.exception.TemplateNotFoundException;
import com.beet.backend.modules.template.domain.model.SlotOptionDomain;
import com.beet.backend.modules.template.domain.model.TemplateDomain;
import com.beet.backend.modules.template.domain.model.TemplateSlotDomain;
import com.beet.backend.modules.template.domain.spi.TemplatePersistencePort;
import com.beet.backend.shared.domain.model.OperationMode;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderUseCase implements OrderServicePort {

    private static final int MONEY_SCALE = 4;
    private static final int RATE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final String DEFAULT_TIME_ZONE = "America/Bogota";
    private static final String PUBLIC_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int PUBLIC_CODE_LENGTH = 6;
    private static final int PUBLIC_CODE_MAX_ATTEMPTS = 3;
    private static final SecureRandom PUBLIC_CODE_RANDOM = new SecureRandom();

    private final OrderPersistencePort orderPersistence;
    private final OrderTaxQueryPort orderTaxQuery;
    private final RestaurantPersistencePort restaurantPersistence;
    private final ItemPersistencePort itemPersistence;
    private final RecipeCalculationServicePort recipeCalculationService;
    private final TemplatePersistencePort templatePersistence;
    private final SubmenuNodeQueryPort submenuNodeQuery;
    private final OrderTableGateway tableGateway;

    @Override
    @Transactional
    public OrderDomain createDraft(OrderDomain order, UUID userId, UUID deviceId) {
        RestaurantDomain restaurant = loadRestaurant(order.getRestaurantId());
        validateServiceContext(order);
        if (order.getServiceType() == ServiceType.DINE_IN) {
            tableGateway.validateAvailableForOrder(order.getRestaurantId(), order.getTableId());
        }

        TaxContext taxContext = resolveTaxContext(restaurant);
        List<OrderItemDomain> preparedItems = buildItems(order.getItems(), restaurant.getId(), userId);
        Totals totals = computeTotals(preparedItems, taxContext.rate(), order.getDeliveryFee());

        order.setOrderStatus(OrderStatus.DRAFT);
        order.setKitchenStatus(KitchenStatus.NOT_SENT);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setOperationModeSnapshot(restaurant.getOperationMode());
        order.setOriginDeviceId(deviceId);
        order.setDeliveryStatus(resolveDeliveryStatus(order.getServiceType()));
        order.setPrepaymentRequiredSnapshot(restaurant.getOperationMode() == OperationMode.PREPAID);
        order.setTaxRateSnapshot(taxContext.rate());
        order.setSubtotalGrossSnapshot(totals.subtotalGross());
        order.setTaxAmountSnapshot(totals.taxAmount());
        order.setTotalGrossSnapshot(totals.totalGross());
        order.setTipTotalSnapshot(BigDecimal.ZERO.setScale(MONEY_SCALE));
        order.setCreatedBy(userId);
        order.setUpdatedBy(userId);
        order.setItems(preparedItems);
        assignFriendlyCodes(order, restaurant);

        OrderDomain saved = orderPersistence.save(order);
        savePreparedItems(saved.getId(), preparedItems, userId);
        refreshTaxes(saved, taxContext, totals, userId);
        return loadOrder(saved.getRestaurantId(), saved.getId());
    }

    @Override
    @Transactional
    public OrderDomain confirmOrder(UUID restaurantId, UUID orderId, UUID userId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        if (order.getOrderStatus() != OrderStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft orders can be confirmed.");
        }

        List<InventoryReservationDomain> reservations = buildReservations(order, userId);
        orderPersistence.saveReservations(reservations);

        boolean prepaid = order.getOperationModeSnapshot() == OperationMode.PREPAID;
        order.setOrderStatus(prepaid ? OrderStatus.AWAITING_PAYMENT : OrderStatus.OPEN);
        order.setKitchenStatus(prepaid ? KitchenStatus.NOT_SENT : KitchenStatus.PENDING);
        order.setUpdatedBy(userId);
        orderPersistence.update(order);

        if (!prepaid) {
            createKitchenTicket(order, userId);
        }
        return loadOrder(restaurantId, orderId);
    }

    @Override
    @Transactional
    public OrderDomain completeOrder(UUID restaurantId, UUID orderId, UUID userId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        if (order.getOrderStatus() != OrderStatus.OPEN) {
            throw new IllegalArgumentException("Only open orders can be completed.");
        }
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalArgumentException("Order must be fully paid before completion.");
        }
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setUpdatedBy(userId);
        orderPersistence.update(order);
        return loadOrder(restaurantId, orderId);
    }

    @Override
    @Transactional
    public OrderDomain cancelOrder(UUID restaurantId, UUID orderId, String reason, UUID userId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed orders cannot be canceled.");
        }
        for (OrderItemDomain item : order.getItems()) {
            orderPersistence.releaseReservationsByOrderItem(item.getId(), userId);
        }
        order.setOrderStatus(OrderStatus.CANCELED);
        order.setKitchenStatus(KitchenStatus.NOT_SENT);
        order.setCancelReason(reason);
        order.setUpdatedBy(userId);
        orderPersistence.update(order);
        return loadOrder(restaurantId, orderId);
    }

    @Override
    @Transactional
    public OrderDomain createOrder(OrderDomain order, UUID userId) {
        OrderDomain draft = createDraft(order, userId, order.getOriginDeviceId());
        return confirmOrder(draft.getRestaurantId(), draft.getId(), userId);
    }

    @Override
    @Transactional
    public OrderDomain addItem(UUID restaurantId, UUID orderId, OrderItemDomain item, UUID userId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        if (order.getOrderStatus() != OrderStatus.OPEN && order.getOrderStatus() != OrderStatus.DRAFT) {
            throw new IllegalArgumentException("Items can only be added to draft or open orders.");
        }

        OrderItemDomain prepared = buildItem(item, restaurantId, userId);
        prepared.setOrderId(orderId);
        OrderItemDomain saved = orderPersistence.saveItem(prepared);
        saved.setIngredientRequirements(prepared.getIngredientRequirements());
        orderPersistence.saveIngredientRequirements(saved);
        orderPersistence.saveTemplateSnapshots(saved);

        List<OrderItemDomain> items = new ArrayList<>(order.getItems());
        items.add(saved);
        order.setItems(items);
        recalculateAndPersist(order, userId);

        if (order.getOrderStatus() == OrderStatus.OPEN) {
            List<InventoryReservationDomain> reservations = buildReservationsForItem(order, saved, userId);
            orderPersistence.saveReservations(reservations);
            createKitchenTicket(order, List.of(saved), userId);
        }
        return loadOrder(restaurantId, orderId);
    }

    @Override
    @Transactional
    public OrderDomain updateItemQuantity(UUID restaurantId, UUID orderId, UUID orderItemId,
            BigDecimal quantity, UUID userId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        if (order.getOrderStatus() != OrderStatus.DRAFT) {
            throw new IllegalArgumentException("Quantity can only be edited while the order is draft.");
        }
        quantity = normalizeQuantity(quantity);
        OrderItemDomain target = findItem(order, orderItemId);
        BigDecimal subtotal = defaulted(target.getUnitPriceSnapshot()).multiply(quantity)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        target.setQuantity(quantity.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        target.setSubtotalGrossSnapshot(subtotal);
        orderPersistence.updateItemQuantity(orderItemId, target.getQuantity(), subtotal, userId);
        recalculateAndPersist(order, userId);
        return loadOrder(restaurantId, orderId);
    }

    @Override
    @Transactional
    public OrderDomain removeItem(UUID restaurantId, UUID orderId, UUID orderItemId, UUID userId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        if (order.getOrderStatus() != OrderStatus.DRAFT && order.getOrderStatus() != OrderStatus.OPEN) {
            throw new IllegalArgumentException("Items can only be removed from draft or open orders.");
        }
        findItem(order, orderItemId);
        orderPersistence.releaseReservationsByOrderItem(orderItemId, userId);
        orderPersistence.deleteItem(orderItemId);
        order.getItems().removeIf(item -> orderItemId.equals(item.getId()));
        recalculateAndPersist(order, userId);
        return loadOrder(restaurantId, orderId);
    }

    @Override
    public Optional<OrderDomain> findById(UUID restaurantId, UUID orderId) {
        return orderPersistence.findByIdWithItems(orderId)
                .filter(order -> restaurantId.equals(order.getRestaurantId()));
    }

    @Override
    public PageResponse<OrderDomain> findAllPaged(OrderSearchCriteria criteria) {
        return orderPersistence.findAllPaged(criteria);
    }

    @Override
    public PageResponse<OrderDomain> findAllPaged(UUID restaurantId, int page, int size, String search) {
        return orderPersistence.findAllPaged(restaurantId, page, size, search);
    }

    @Override
    public PageResponse<PosCatalogEntryDomain> findPosCatalog(UUID restaurantId, int page, int size, String search,
            UUID menuId, UUID submenuId, String availability, String referenceType, String sort) {
        PageResponse<PosCatalogEntryDomain> result = orderPersistence.findPosCatalog(
                restaurantId, page, size, search, menuId, submenuId, availability, referenceType, sort);
        result.content().forEach(entry -> enrichAvailability(restaurantId, entry));
        return result;
    }

    private void enrichAvailability(UUID restaurantId, PosCatalogEntryDomain entry) {
        if (entry.getReferenceType() == com.beet.backend.modules.order.domain.model.CatalogReferenceType.PRODUCT) {
            ProductAvailability availability = calculateProductAvailability(restaurantId, entry.getReferenceId());
            applyAvailability(entry, availability);
            return;
        }

        boolean templateAvailable = entry.isAvailable();
        boolean lowStock = false;
        Integer templateMax = null;
        for (var slot : entry.getSlots()) {
            int availableCapacity = 0;
            int slotUnitCapacity = 0;
            for (var option : slot.getOptions()) {
                ProductAvailability optionAvailability =
                        calculateProductAvailability(restaurantId, option.getItemId());
                option.setAvailable(option.isAvailable() && optionAvailability.available());
                option.setLowStock(optionAvailability.lowStock());
                option.setMaxAvailableUnits(optionAvailability.maxAvailableUnits());
                option.setInsufficientIngredients(optionAvailability.insufficientIngredients());
                if (!option.isAvailable()) {
                    option.setUnavailableReason(optionAvailability.reason());
                }
                lowStock = lowStock || option.isLowStock();
                if (option.isAvailable()) {
                    int capacity = option.getMaxAvailableUnits() == null
                            ? option.getMaxQuantity()
                            : Math.min(option.getMaxQuantity(), option.getMaxAvailableUnits());
                    availableCapacity += capacity;
                    slotUnitCapacity += capacity;
                }
            }
            if (slot.getMinSelection() > 0) {
                if (availableCapacity < slot.getMinSelection()) {
                    templateAvailable = false;
                } else {
                    int slotTemplates = slotUnitCapacity / slot.getMinSelection();
                    templateMax = templateMax == null ? slotTemplates : Math.min(templateMax, slotTemplates);
                }
            }
        }
        entry.setAvailable(templateAvailable);
        entry.setLowStock(lowStock);
        entry.setMaxAvailableUnits(templateMax);
        if (!templateAvailable && entry.getUnavailableReason() == null) {
            entry.setUnavailableReason("No hay opciones suficientes para completar los slots obligatorios");
        }
    }

    private void applyAvailability(PosCatalogEntryDomain entry, ProductAvailability availability) {
        entry.setAvailable(entry.isAvailable() && availability.available());
        entry.setLowStock(availability.lowStock());
        entry.setMaxAvailableUnits(availability.maxAvailableUnits());
        entry.setInsufficientIngredients(availability.insufficientIngredients());
        if (!entry.isAvailable() && entry.getUnavailableReason() == null) {
            entry.setUnavailableReason(availability.reason());
        }
    }

    private ProductAvailability calculateProductAvailability(UUID restaurantId, UUID productId) {
        ItemDomain product = loadProduct(restaurantId, productId);
        if (!product.isInventoryTracked()) {
            return new ProductAvailability(true, false, null, null, List.of());
        }
        RecipeCalculationResult calculation = recipeCalculationService.calculate(restaurantId, productId);
        int maxAvailable = Integer.MAX_VALUE;
        boolean lowStock = false;
        List<String> insufficient = new ArrayList<>();
        for (RecipeIngredientRequirement requirement :
                calculation.ingredientRequirementsPerSellableUnit()) {
            Optional<IngredientStockAvailabilityDomain> stock =
                    orderPersistence.findIngredientStockAvailability(
                            restaurantId, requirement.masterIngredientId());
            if (stock.isEmpty()) {
                insufficient.add(requirement.ingredientName());
                maxAvailable = 0;
                continue;
            }
            IngredientStockAvailabilityDomain snapshot = stock.get();
            lowStock = lowStock || snapshot.currentStock().compareTo(snapshot.minStock()) <= 0;
            int ingredientMax = snapshot.availableStock()
                    .divideToIntegralValue(requirement.quantityBase())
                    .max(BigDecimal.ZERO)
                    .intValue();
            maxAvailable = Math.min(maxAvailable, ingredientMax);
            if (ingredientMax < 1) {
                insufficient.add(requirement.ingredientName());
            }
        }
        Integer max = maxAvailable == Integer.MAX_VALUE ? null : maxAvailable;
        boolean available = max == null || max > 0;
        String reason = available ? null : "Inventario insuficiente";
        return new ProductAvailability(available, lowStock, max, reason, List.copyOf(insufficient));
    }

    @Override
    @Transactional
    public KitchenTicketDomain updateKitchenTicketStatus(UUID restaurantId, UUID ticketId, KitchenTicketStatus status,
            UUID userId) {
        KitchenTicketDomain current = orderPersistence.findKitchenTicket(restaurantId, ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Kitchen ticket not found."));
        if (status == null) {
            throw new IllegalArgumentException("Kitchen ticket status is required.");
        }
        validateKitchenTransition(current.getStatus(), status);
        if (status == KitchenTicketStatus.PREPARING) {
            orderPersistence.consumeReservationsByTicket(restaurantId, ticketId, userId);
        }
        if (status == KitchenTicketStatus.CANCELED && current.getStatus() == KitchenTicketStatus.PENDING) {
            for (KitchenTicketLineDomain line : current.getLines()) {
                orderPersistence.releaseReservationsByOrderItem(line.getOrderItemId(), userId);
            }
        }
        KitchenTicketDomain updated = orderPersistence.updateKitchenTicketStatus(restaurantId, ticketId, status, userId);
        refreshKitchenSummary(restaurantId, updated.getOrderId(), userId);
        return updated;
    }

    @Override
    public PageResponse<KitchenTicketDomain> listKitchenTickets(UUID restaurantId, KitchenTicketStatus status,
            int page, int size) {
        return orderPersistence.findKitchenTickets(restaurantId, status, page, size);
    }

    @Override
    public List<PaymentMethodDomain> listPaymentMethods(UUID restaurantId) {
        return orderPersistence.findPaymentMethods(restaurantId);
    }

    @Override
    @Transactional
    public PaymentMethodDomain createPaymentMethod(PaymentMethodDomain method, UUID userId) {
        method.setCreatedBy(userId);
        method.setUpdatedBy(userId);
        return orderPersistence.savePaymentMethod(method);
    }

    @Override
    @Transactional
    public PaymentMethodDomain updatePaymentMethod(UUID restaurantId, UUID methodId, Boolean isActive,
            String name, Boolean requiresReference, Integer sortOrder, UUID userId) {
        PaymentMethodDomain method = orderPersistence.findPaymentMethod(restaurantId, methodId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found."));
        if (isActive != null) {
            method.setActive(isActive);
        }
        if (name != null && !name.isBlank()) {
            method.setName(name.trim());
        }
        if (requiresReference != null) {
            method.setRequiresReference(requiresReference);
        }
        if (sortOrder != null) {
            method.setSortOrder(sortOrder);
        }
        method.setUpdatedBy(userId);
        return orderPersistence.updatePaymentMethod(method);
    }

    @Override
    @Transactional
    public PaymentDomain registerPayment(UUID restaurantId, UUID orderId, PaymentDomain payment,
            UUID userId, UUID deviceId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        PaymentMethodDomain method = orderPersistence.findPaymentMethod(restaurantId, payment.getPaymentMethodId())
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found."));
        if (!method.isActive()) {
            throw new IllegalArgumentException("Payment method is inactive.");
        }
        if (method.isRequiresReference()
                && (payment.getExternalReference() == null || payment.getExternalReference().isBlank())) {
            throw new IllegalArgumentException("Payment method requires an external reference.");
        }
        payment.setRestaurantId(restaurantId);
        payment.setOrderId(orderId);
        payment.setDeviceId(deviceId);
        payment.setStatus(PaymentRecordStatus.RECORDED);
        payment.setCreatedBy(userId);
        PaymentDomain saved = orderPersistence.savePayment(payment);

        BigDecimal paid = orderPersistence.sumRecordedPayments(orderId);
        BigDecimal tips = orderPersistence.sumRecordedTips(orderId);
        order.setTipTotalSnapshot(tips);
        order.setPaymentStatus(resolvePaymentStatus(order, paid));
        if (order.getOrderStatus() == OrderStatus.AWAITING_PAYMENT
                && order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setOrderStatus(OrderStatus.OPEN);
            order.setKitchenStatus(KitchenStatus.PENDING);
            createKitchenTicket(order, userId);
        }
        order.setUpdatedBy(userId);
        orderPersistence.update(order);
        return saved;
    }

    private void recalculateAndPersist(OrderDomain order, UUID userId) {
        RestaurantDomain restaurant = loadRestaurant(order.getRestaurantId());
        TaxContext taxContext = resolveTaxContext(restaurant);
        Totals totals = computeTotals(order.getItems(), taxContext.rate(), order.getDeliveryFee());
        order.setTaxRateSnapshot(taxContext.rate());
        order.setSubtotalGrossSnapshot(totals.subtotalGross());
        order.setTaxAmountSnapshot(totals.taxAmount());
        order.setTotalGrossSnapshot(totals.totalGross());
        order.setUpdatedBy(userId);
        orderPersistence.update(order);
        refreshTaxes(order, taxContext, totals, userId);
    }

    private void savePreparedItems(UUID orderId, List<OrderItemDomain> preparedItems, UUID userId) {
        List<OrderItemDomain> savedItems = new ArrayList<>();
        for (OrderItemDomain item : preparedItems) {
            item.setOrderId(orderId);
            item.setCreatedBy(userId);
            item.setUpdatedBy(userId);
            OrderItemDomain saved = orderPersistence.saveItem(item);
            saved.setIngredientRequirements(item.getIngredientRequirements());
            orderPersistence.saveIngredientRequirements(saved);
            saved.setTemplateSlots(item.getTemplateSlots());
            orderPersistence.saveTemplateSnapshots(saved);
            savedItems.add(saved);
        }
        preparedItems.clear();
        preparedItems.addAll(savedItems);
    }

    private void refreshTaxes(OrderDomain order, TaxContext taxContext, Totals totals, UUID userId) {
        List<OrderTaxDomain> orderTaxes = buildOrderTaxes(order.getId(), taxContext, totals.subtotalGross(), userId);
        orderPersistence.replaceOrderTaxes(order.getId(), orderTaxes);
        List<OrderItemTaxDomain> itemTaxes = buildOrderItemTaxes(order.getItems(), taxContext, userId);
        orderPersistence.replaceOrderItemTaxes(order.getId(), itemTaxes);
    }

    private List<OrderItemDomain> buildItems(List<OrderItemDomain> items, UUID restaurantId, UUID userId) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must include at least one item.");
        }
        List<OrderItemDomain> prepared = new ArrayList<>();
        for (OrderItemDomain item : items) {
            prepared.add(buildItem(item, restaurantId, userId));
        }
        return prepared;
    }

    private OrderItemDomain buildItem(OrderItemDomain item, UUID restaurantId, UUID userId) {
        OrderLineType lineType = item.getLineType() != null ? item.getLineType() : OrderLineType.PRODUCT;
        if (lineType == OrderLineType.PRODUCT) {
            return buildProductItem(item, restaurantId, userId);
        }
        return buildTemplateItem(item, restaurantId, userId);
    }

    private OrderItemDomain buildProductItem(OrderItemDomain item, UUID restaurantId, UUID userId) {
        if (item.getItemId() == null) {
            throw new IllegalArgumentException("Product order item must include itemId.");
        }
        ItemDomain sourceItem = loadProduct(restaurantId, item.getItemId());
        validateSubmenuNode(item, sourceItem.getId(), null);
        BigDecimal quantity = normalizeQuantity(item.getQuantity());
        BigDecimal unitPrice = defaulted(sourceItem.getSalePrice());
        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Published product must have a sale price.");
        }
        RecipeCalculationResult calculation = sourceItem.isInventoryTracked()
                ? recipeCalculationService.calculate(restaurantId, sourceItem.getId())
                : null;
        return OrderItemDomain.builder()
                .lineType(OrderLineType.PRODUCT)
                .itemId(sourceItem.getId())
                .submenuNodeId(item.getSubmenuNodeId())
                .itemNameSnapshot(sourceItem.getName())
                .unitPriceSnapshot(unitPrice)
                .theoreticalCostSnapshot(calculation != null
                        ? calculation.costPerSellableUnit()
                        : sourceItem.getTheoreticalCost())
                .quantity(quantity)
                .subtotalGrossSnapshot(unitPrice.multiply(quantity).setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .notes(item.getNotes())
                .ingredientRequirements(calculation == null
                        ? List.of()
                        : toOrderRequirements(restaurantId, calculation.ingredientRequirementsPerSellableUnit()))
                .createdBy(userId)
                .updatedBy(userId)
                .build();
    }

    private OrderItemDomain buildTemplateItem(OrderItemDomain item, UUID restaurantId, UUID userId) {
        if (item.getTemplateId() == null) {
            throw new IllegalArgumentException("Template order item must include templateId.");
        }
        TemplateDomain template = templatePersistence.findById(item.getTemplateId())
                .orElseThrow(() -> TemplateNotFoundException.forId(item.getTemplateId()));
        if (!restaurantId.equals(template.getRestaurantId()) || !template.isActive()) {
            throw new IllegalArgumentException("Template is not available for this restaurant.");
        }
        validateSubmenuNode(item, null, item.getTemplateId());
        BigDecimal quantity = normalizeQuantity(item.getQuantity());
        List<OrderItemTemplateSlotDomain> slotSnapshots = buildTemplateSlotSnapshots(template, item);
        BigDecimal surchargeTotal = slotSnapshots.stream()
                .flatMap(slot -> slot.getOptions().stream())
                .map(option -> defaulted(option.getSurchargeSnapshot()).multiply(normalizeQuantity(option.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unitPrice = defaulted(template.getBasePrice()).add(surchargeTotal);
        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Template item must have a positive price.");
        }
        List<OrderItemIngredientRequirementDomain> requirements =
                buildTemplateRequirements(restaurantId, slotSnapshots);
        return OrderItemDomain.builder()
                .lineType(OrderLineType.TEMPLATE)
                .templateId(item.getTemplateId())
                .submenuNodeId(item.getSubmenuNodeId())
                .itemNameSnapshot(template.getName())
                .unitPriceSnapshot(unitPrice)
                .theoreticalCostSnapshot(calculateTemplateCost(slotSnapshots))
                .quantity(quantity)
                .subtotalGrossSnapshot(unitPrice.multiply(quantity).setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .notes(item.getNotes())
                .templateSlots(slotSnapshots)
                .ingredientRequirements(requirements)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
    }

    private List<OrderItemTemplateSlotDomain> buildTemplateSlotSnapshots(TemplateDomain template, OrderItemDomain item) {
        if (item.getTemplateSlots() == null || item.getTemplateSlots().isEmpty()) {
            throw new IllegalArgumentException("Template order item must include slot selections.");
        }
        List<OrderItemTemplateSlotDomain> snapshots = new ArrayList<>();
        for (TemplateSlotDomain slot : template.getSlots()) {
            OrderItemTemplateSlotDomain selection = item.getTemplateSlots().stream()
                    .filter(candidate -> slot.getId().equals(candidate.getTemplateSlotId()))
                    .findFirst()
                    .orElse(null);
            int selectedUnits = selection == null ? 0 : selection.getOptions().stream()
                    .map(option -> normalizeQuantity(option.getQuantity()).intValue())
                    .reduce(0, Integer::sum);
            if (selectedUnits < slot.getMinSelection() || selectedUnits > slot.getMaxSelection()) {
                throw new IllegalArgumentException("Invalid selection count for slot: " + slot.getName());
            }
            List<OrderItemTemplateOptionDomain> options = new ArrayList<>();
            if (selection != null) {
                for (OrderItemTemplateOptionDomain selected : selection.getOptions()) {
                    SlotOptionDomain sourceOption = slot.getOptions().stream()
                            .filter(option -> option.getId().equals(selected.getSlotOptionId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Invalid slot option selected."));
                    BigDecimal selectedQuantity = normalizeQuantity(selected.getQuantity());
                    if (selectedQuantity.intValue() > sourceOption.getMaxQuantity()) {
                        throw new IllegalArgumentException("Selected quantity exceeds option max quantity.");
                    }
                    ItemDomain product = loadProduct(template.getRestaurantId(), sourceOption.getItemId());
                    options.add(OrderItemTemplateOptionDomain.builder()
                            .slotOptionId(sourceOption.getId())
                            .itemId(product.getId())
                            .itemNameSnapshot(product.getName())
                            .quantity(selectedQuantity)
                            .surchargeSnapshot(defaulted(sourceOption.getSurcharge()))
                            .theoreticalCostSnapshot(product.isInventoryTracked()
                                    ? recipeCalculationService.calculate(
                                            template.getRestaurantId(), product.getId()).costPerSellableUnit()
                                    : product.getTheoreticalCost())
                            .build());
                }
            }
            snapshots.add(OrderItemTemplateSlotDomain.builder()
                    .templateSlotId(slot.getId())
                    .slotNameSnapshot(slot.getName())
                    .minSelectionSnapshot(slot.getMinSelection())
                    .maxSelectionSnapshot(slot.getMaxSelection())
                    .sortOrder(slot.getSortOrder())
                    .options(options)
                    .build());
        }
        return snapshots;
    }

    private ItemDomain loadProduct(UUID restaurantId, UUID itemId) {
        ItemDomain item = itemPersistence.findById(itemId)
                .orElseThrow(() -> ItemNotFoundException.forId(itemId));
        if (!restaurantId.equals(item.getRestaurantId()) || item.getItemClass() != ItemClass.PRODUCT || !item.isActive()) {
            throw new IllegalArgumentException("Product is not available for this restaurant.");
        }
        item.setRecipeLines(itemPersistence.findRecipeLinesByParent(item.getId()));
        return item;
    }

    private void validateSubmenuNode(OrderItemDomain item, UUID itemId, UUID templateId) {
        if (item.getSubmenuNodeId() == null) {
            throw new IllegalArgumentException("Order items must come from a published submenu node.");
        }
        SubmenuNodeDomain node = submenuNodeQuery.findNodeById(item.getSubmenuNodeId())
                .orElseThrow(() -> new IllegalArgumentException("Submenu node not found."));
        if (itemId != null) {
            if (node.nodeType() != SubmenuNodeType.PRODUCT || !itemId.equals(node.itemId())) {
                throw new IllegalArgumentException("Submenu node does not match the product.");
            }
            return;
        }
        if (node.nodeType() != SubmenuNodeType.TEMPLATE || !templateId.equals(node.templateId())) {
            throw new IllegalArgumentException("Submenu node does not match the template.");
        }
    }

    private void validateServiceContext(OrderDomain order) {
        if (order.getServiceType() == null) {
            throw new IllegalArgumentException("Order must include a service type.");
        }
        if (order.getServiceType() == ServiceType.DINE_IN && order.getTableId() == null) {
            throw new IllegalArgumentException("Dine-in order must include a restaurant table.");
        }
        if (order.getServiceType() != ServiceType.DINE_IN && order.getTableId() != null) {
            throw new IllegalArgumentException("Only dine-in orders can include a restaurant table.");
        }
        if (order.getServiceType() == ServiceType.DELIVERY) {
            if (isBlank(order.getDeliveryAddress()) || isBlank(order.getDeliveryPhone())) {
                throw new IllegalArgumentException("Delivery orders require address and phone.");
            }
        }
    }

    private List<InventoryReservationDomain> buildReservations(OrderDomain order, UUID userId) {
        List<InventoryReservationDomain> reservations = new ArrayList<>();
        for (OrderItemDomain item : order.getItems()) {
            reservations.addAll(buildReservationsForItem(order, item, userId));
        }
        reservations.sort(Comparator
                .comparing((InventoryReservationDomain reservation) ->
                        reservation.getMasterIngredientId().toString())
                .thenComparing(reservation -> reservation.getOrderItemId().toString()));
        return reservations;
    }

    private List<InventoryReservationDomain> buildReservationsForItem(OrderDomain order, OrderItemDomain item, UUID userId) {
        List<OrderItemIngredientRequirementDomain> requirements =
                item.getIngredientRequirements() == null || item.getIngredientRequirements().isEmpty()
                        ? orderPersistence.findIngredientRequirements(item.getId())
                        : item.getIngredientRequirements();
        return requirements.stream()
                .sorted(Comparator.comparing(requirement -> requirement.getMasterIngredientId().toString()))
                .map(requirement -> InventoryReservationDomain.builder()
                        .restaurantId(order.getRestaurantId())
                        .orderId(order.getId())
                        .orderItemId(item.getId())
                        .masterIngredientId(requirement.getMasterIngredientId())
                        .quantityBase(requirement.getQuantityBasePerSaleUnit()
                                .multiply(item.getQuantity())
                                .setScale(6, RoundingMode.HALF_UP))
                        .unitCostSnapshot(requirement.getUnitCostSnapshot())
                        .status(InventoryReservationStatus.ACTIVE)
                        .createdBy(userId)
                        .updatedBy(userId)
                        .build())
                .toList();
    }

    private KitchenTicketDomain createKitchenTicket(OrderDomain order, UUID userId) {
        return createKitchenTicket(order, order.getItems(), userId);
    }

    private KitchenTicketDomain createKitchenTicket(OrderDomain order, List<OrderItemDomain> items, UUID userId) {
        KitchenTicketDomain ticket = KitchenTicketDomain.builder()
                .restaurantId(order.getRestaurantId())
                .orderId(order.getId())
                .status(KitchenTicketStatus.PENDING)
                .sentBy(userId)
                .lines(items.stream()
                        .map(item -> KitchenTicketLineDomain.builder()
                                .orderItemId(item.getId())
                                .quantity(item.getQuantity())
                                .itemNameSnapshot(item.getItemNameSnapshot())
                                .notes(item.getNotes())
                                .build())
                        .toList())
                .build();
        return orderPersistence.saveKitchenTicket(ticket);
    }

    private void validateKitchenTransition(KitchenTicketStatus current, KitchenTicketStatus target) {
        if (current == target) {
            return;
        }
        if (current == KitchenTicketStatus.CANCELED || current == KitchenTicketStatus.READY) {
            throw new IllegalArgumentException("Final kitchen tickets cannot change status.");
        }
        if (current == KitchenTicketStatus.PENDING
                && (target == KitchenTicketStatus.PREPARING || target == KitchenTicketStatus.CANCELED)) {
            return;
        }
        if (current == KitchenTicketStatus.PREPARING
                && (target == KitchenTicketStatus.READY || target == KitchenTicketStatus.CANCELED)) {
            return;
        }
        throw new IllegalArgumentException("Invalid kitchen ticket status transition.");
    }

    private void refreshKitchenSummary(UUID restaurantId, UUID orderId, UUID userId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        List<KitchenTicketStatus> statuses = order.getKitchenTickets().stream()
                .map(KitchenTicketDomain::getStatus)
                .filter(status -> status != KitchenTicketStatus.CANCELED)
                .toList();
        KitchenStatus summary;
        if (statuses.isEmpty()) {
            summary = KitchenStatus.NOT_SENT;
        } else if (statuses.stream().allMatch(status -> status == KitchenTicketStatus.READY)) {
            summary = KitchenStatus.READY;
        } else if (statuses.stream().anyMatch(status -> status == KitchenTicketStatus.READY)) {
            summary = KitchenStatus.PARTIALLY_READY;
        } else if (statuses.stream().anyMatch(status -> status == KitchenTicketStatus.PREPARING)) {
            summary = KitchenStatus.PREPARING;
        } else {
            summary = KitchenStatus.PENDING;
        }
        order.setKitchenStatus(summary);
        order.setUpdatedBy(userId);
        orderPersistence.update(order);
    }

    private RestaurantDomain loadRestaurant(UUID restaurantId) {
        return restaurantPersistence.findById(restaurantId)
                .orElseThrow(() -> RestaurantNotFoundException.forId(restaurantId));
    }

    private OrderDomain loadOrder(UUID restaurantId, UUID orderId) {
        return findById(restaurantId, orderId)
                .orElseThrow(() -> OrderNotFoundException.forId(orderId));
    }

    private OrderItemDomain findItem(OrderDomain order, UUID orderItemId) {
        return order.getItems().stream()
                .filter(item -> orderItemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> OrderItemNotFoundException.forId(orderItemId));
    }

    private PaymentStatus resolvePaymentStatus(OrderDomain order, BigDecimal paid) {
        BigDecimal total = defaulted(order.getTotalGrossSnapshot());
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentStatus.UNPAID;
        }
        if (paid.compareTo(total) >= 0) {
            return PaymentStatus.PAID;
        }
        return PaymentStatus.PARTIALLY_PAID;
    }

    private DeliveryStatus resolveDeliveryStatus(ServiceType serviceType) {
        return serviceType == ServiceType.DELIVERY ? DeliveryStatus.PENDING_DISPATCH : DeliveryStatus.NOT_APPLICABLE;
    }

    private Totals computeTotals(List<OrderItemDomain> items, BigDecimal rate, BigDecimal deliveryFee) {
        BigDecimal subtotal = items.stream()
                .map(OrderItemDomain::getSubtotalGrossSnapshot)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(defaulted(deliveryFee))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal taxAmount = calculateTaxFromGross(subtotal, rate);
        return new Totals(subtotal, taxAmount, subtotal);
    }

    private BigDecimal calculateTaxFromGross(BigDecimal gross, BigDecimal rate) {
        if (gross == null || rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return gross.multiply(rate)
                .divide(rate.add(ONE_HUNDRED), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private TaxContext resolveTaxContext(RestaurantDomain restaurant) {
        List<OrderTaxDefinition> definitions = orderTaxQuery.findActiveTaxesByRestaurant(restaurant.getId());
        BigDecimal rate = definitions.stream()
                .map(OrderTaxDefinition::rate)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (definitions.isEmpty()) {
            BigDecimal fallback = restaurant.getSettings() != null
                    ? restaurant.getSettings().defaultTaxPercentage()
                    : null;
            if (fallback != null) {
                rate = fallback;
                definitions = List.of(new OrderTaxDefinition(null, "Default Tax", fallback));
            }
        }

        return new TaxContext(definitions, normalizeRate(rate));
    }

    private List<OrderTaxDomain> buildOrderTaxes(UUID orderId, TaxContext taxContext,
            BigDecimal baseGross, UUID userId) {
        if (taxContext.definitions().isEmpty() || taxContext.rate().compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        List<OrderTaxDomain> taxes = new ArrayList<>();
        for (OrderTaxDefinition definition : taxContext.definitions()) {
            BigDecimal rate = normalizeRate(definition.rate());
            taxes.add(OrderTaxDomain.builder()
                    .orderId(orderId)
                    .taxId(definition.id())
                    .taxNameSnapshot(definition.name())
                    .taxRateSnapshot(rate)
                    .taxBaseSnapshot(baseGross)
                    .taxAmountSnapshot(calculateTaxFromGross(baseGross, rate))
                    .createdBy(userId)
                    .build());
        }
        return taxes;
    }

    private List<OrderItemTaxDomain> buildOrderItemTaxes(List<OrderItemDomain> items,
            TaxContext taxContext, UUID userId) {
        if (taxContext.definitions().isEmpty() || taxContext.rate().compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        List<OrderItemTaxDomain> taxes = new ArrayList<>();
        for (OrderItemDomain item : items) {
            BigDecimal baseGross = defaulted(item.getSubtotalGrossSnapshot());
            for (OrderTaxDefinition definition : taxContext.definitions()) {
                BigDecimal rate = normalizeRate(definition.rate());
                taxes.add(OrderItemTaxDomain.builder()
                        .orderItemId(item.getId())
                        .taxId(definition.id())
                        .taxNameSnapshot(definition.name())
                        .taxRateSnapshot(rate)
                        .taxBaseSnapshot(baseGross)
                        .taxAmountSnapshot(calculateTaxFromGross(baseGross, rate))
                        .createdBy(userId)
                        .build());
            }
        }
        return taxes;
    }

    private List<OrderItemIngredientRequirementDomain> toOrderRequirements(
            UUID restaurantId, List<RecipeIngredientRequirement> requirements) {
        return requirements.stream()
                .map(requirement -> OrderItemIngredientRequirementDomain.builder()
                        .restaurantId(restaurantId)
                        .masterIngredientId(requirement.masterIngredientId())
                        .ingredientNameSnapshot(requirement.ingredientName())
                        .quantityBasePerSaleUnit(requirement.quantityBase())
                        .unitCostSnapshot(requirement.unitCost())
                        .build())
                .toList();
    }

    private List<OrderItemIngredientRequirementDomain> buildTemplateRequirements(
            UUID restaurantId, List<OrderItemTemplateSlotDomain> slots) {
        Map<UUID, OrderItemIngredientRequirementDomain> aggregated = new LinkedHashMap<>();
        for (OrderItemTemplateOptionDomain option : slots.stream()
                .flatMap(slot -> slot.getOptions().stream())
                .toList()) {
            ItemDomain product = loadProduct(restaurantId, option.getItemId());
            if (!product.isInventoryTracked()) {
                continue;
            }
            RecipeCalculationResult calculation = recipeCalculationService.calculate(restaurantId, option.getItemId());
            BigDecimal selectedQuantity = normalizeQuantity(option.getQuantity());
            for (RecipeIngredientRequirement requirement :
                    calculation.ingredientRequirementsPerSellableUnit()) {
                BigDecimal quantity = requirement.quantityBase()
                        .multiply(selectedQuantity)
                        .setScale(6, RoundingMode.HALF_UP);
                aggregated.compute(requirement.masterIngredientId(), (id, current) -> {
                    if (current == null) {
                        return OrderItemIngredientRequirementDomain.builder()
                                .restaurantId(restaurantId)
                                .masterIngredientId(id)
                                .ingredientNameSnapshot(requirement.ingredientName())
                                .quantityBasePerSaleUnit(quantity)
                                .unitCostSnapshot(requirement.unitCost())
                                .build();
                    }
                    current.setQuantityBasePerSaleUnit(
                            current.getQuantityBasePerSaleUnit().add(quantity).setScale(6, RoundingMode.HALF_UP));
                    if (requirement.unitCost() == null) {
                        current.setUnitCostSnapshot(null);
                    }
                    return current;
                });
            }
        }
        return aggregated.values().stream()
                .sorted(Comparator.comparing(OrderItemIngredientRequirementDomain::getIngredientNameSnapshot))
                .toList();
    }

    private BigDecimal calculateTemplateCost(List<OrderItemTemplateSlotDomain> slots) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemTemplateOptionDomain option : slots.stream()
                .flatMap(slot -> slot.getOptions().stream())
                .toList()) {
            if (option.getTheoreticalCostSnapshot() == null) {
                return null;
            }
            total = total.add(option.getTheoreticalCostSnapshot().multiply(normalizeQuantity(option.getQuantity())));
        }
        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        try {
            return new BigDecimal(quantity.toBigIntegerExact()).setScale(0);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Products and template options require whole quantities.");
        }
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return rate.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal defaulted(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void assignFriendlyCodes(OrderDomain order, RestaurantDomain restaurant) {
        ZoneId zoneId = resolveRestaurantZoneId(restaurant);
        LocalDate businessDate = LocalDate.now(zoneId);
        int dailySequence = orderPersistence.nextDailySequence(order.getRestaurantId(), businessDate);
        order.setBusinessDate(businessDate);
        order.setDailySequence(dailySequence);
        order.setPublicCode(generateUniquePublicCode(order.getRestaurantId()));
    }

    private String generateUniquePublicCode(UUID restaurantId) {
        for (int attempt = 0; attempt < PUBLIC_CODE_MAX_ATTEMPTS; attempt++) {
            String code = randomPublicCode();
            if (!orderPersistence.existsPublicCode(restaurantId, code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique public order code.");
    }

    private String randomPublicCode() {
        StringBuilder code = new StringBuilder(PUBLIC_CODE_LENGTH);
        for (int index = 0; index < PUBLIC_CODE_LENGTH; index++) {
            code.append(PUBLIC_CODE_ALPHABET.charAt(PUBLIC_CODE_RANDOM.nextInt(PUBLIC_CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private ZoneId resolveRestaurantZoneId(RestaurantDomain restaurant) {
        String timeZone = restaurant.getSettings() == null ? null : restaurant.getSettings().timeZone();
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneId.of(DEFAULT_TIME_ZONE);
        }
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Invalid restaurant time zone: " + timeZone);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record TaxContext(List<OrderTaxDefinition> definitions, BigDecimal rate) {
    }

    private record Totals(BigDecimal subtotalGross, BigDecimal taxAmount, BigDecimal totalGross) {
    }

    private record ProductAvailability(
            boolean available,
            boolean lowStock,
            Integer maxAvailableUnits,
            String reason,
            List<String> insufficientIngredients) {
    }

}
