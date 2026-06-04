package com.beet.backend.modules.order.domain.usecase;

import com.beet.backend.modules.item.domain.exception.ItemNotFoundException;
import com.beet.backend.modules.item.domain.model.ItemClass;
import com.beet.backend.modules.item.domain.model.ItemDomain;
import com.beet.backend.modules.item.domain.spi.ItemPersistencePort;
import com.beet.backend.modules.menu.domain.model.SubmenuNodeDomain;
import com.beet.backend.modules.menu.domain.model.SubmenuNodeType;
import com.beet.backend.modules.menu.domain.spi.SubmenuNodeQueryPort;
import com.beet.backend.modules.order.domain.api.OrderServicePort;
import com.beet.backend.modules.order.domain.exception.OrderItemNotFoundException;
import com.beet.backend.modules.order.domain.exception.OrderNotFoundException;
import com.beet.backend.modules.order.domain.model.KitchenStatus;
import com.beet.backend.modules.order.domain.model.OrderDomain;
import com.beet.backend.modules.order.domain.model.OrderItemDomain;
import com.beet.backend.modules.order.domain.model.OrderItemTaxDomain;
import com.beet.backend.modules.order.domain.model.OrderStatus;
import com.beet.backend.modules.order.domain.model.OrderTaxDefinition;
import com.beet.backend.modules.order.domain.model.OrderTaxDomain;
import com.beet.backend.modules.order.domain.model.PaymentStatus;
import com.beet.backend.modules.order.domain.model.ServiceType;
import com.beet.backend.modules.order.domain.spi.OrderPersistencePort;
import com.beet.backend.modules.order.domain.spi.OrderTableGateway;
import com.beet.backend.modules.order.domain.spi.OrderTaxQueryPort;
import com.beet.backend.modules.restaurant.domain.exception.RestaurantNotFoundException;
import com.beet.backend.modules.restaurant.domain.model.RestaurantDomain;
import com.beet.backend.modules.restaurant.domain.spi.RestaurantPersistencePort;
import com.beet.backend.shared.domain.model.OperationMode;
import com.beet.backend.shared.infrastructure.input.rest.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderUseCase implements OrderServicePort {

    private static final int MONEY_SCALE = 4;
    private static final int RATE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final OrderPersistencePort orderPersistence;
    private final OrderTaxQueryPort orderTaxQuery;
    private final RestaurantPersistencePort restaurantPersistence;
    private final ItemPersistencePort itemPersistence;
    private final SubmenuNodeQueryPort submenuNodeQuery;
    private final OrderTableGateway tableGateway;

    @Override
    @Transactional
    public OrderDomain createOrder(OrderDomain order, UUID userId) {
        RestaurantDomain restaurant = loadRestaurant(order.getRestaurantId());
        validateTableContext(order);
        if (order.getCashSessionId() == null) {
            throw new IllegalArgumentException("Order must be linked to an active cash session.");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must include at least one item.");
        }

        TaxContext taxContext = resolveTaxContext(restaurant);
        List<OrderItemDomain> preparedItems = buildItems(order.getItems(), restaurant.getId(), userId);
        Totals totals = computeTotals(preparedItems, taxContext.rate());

        order.setOrderStatus(OrderStatus.OPEN);
        order.setKitchenStatus(KitchenStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setPrepaymentRequiredSnapshot(resolvePrepaymentRequired(restaurant));
        order.setTaxRateSnapshot(taxContext.rate());
        order.setSubtotalGrossSnapshot(totals.subtotalGross());
        order.setTaxAmountSnapshot(totals.taxAmount());
        order.setTotalGrossSnapshot(totals.totalGross());
        order.setCreatedBy(userId);
        order.setUpdatedBy(userId);
        order.setItems(preparedItems);

        OrderDomain saved = orderPersistence.save(order);
        UUID orderId = saved.getId();

        List<OrderItemDomain> savedItems = new ArrayList<>();
        for (OrderItemDomain item : preparedItems) {
            item.setOrderId(orderId);
            item.setCreatedBy(userId);
            item.setUpdatedBy(userId);
            savedItems.add(orderPersistence.saveItem(item));
        }

        List<OrderTaxDomain> orderTaxes = buildOrderTaxes(orderId, taxContext, totals.subtotalGross(), userId);
        orderPersistence.replaceOrderTaxes(orderId, orderTaxes);

        List<OrderItemTaxDomain> itemTaxes = buildOrderItemTaxes(savedItems, taxContext, userId);
        orderPersistence.replaceOrderItemTaxes(orderId, itemTaxes);
        applyItemTaxes(savedItems, itemTaxes);

        saved.setItems(savedItems);
        saved.setTaxes(orderTaxes);
        return saved;
    }

    @Override
    @Transactional
    public OrderDomain addItem(UUID restaurantId, UUID orderId, OrderItemDomain item, UUID userId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        RestaurantDomain restaurant = loadRestaurant(order.getRestaurantId());
        TaxContext taxContext = resolveTaxContext(restaurant);

        OrderItemDomain prepared = buildItem(item, restaurant.getId(), userId);
        prepared.setOrderId(orderId);

        OrderItemDomain savedItem = orderPersistence.saveItem(prepared);
        List<OrderItemDomain> items = new ArrayList<>(order.getItems());
        items.add(savedItem);

        Totals totals = computeTotals(items, taxContext.rate());
        order.setItems(items);
        order.setTaxRateSnapshot(taxContext.rate());
        order.setSubtotalGrossSnapshot(totals.subtotalGross());
        order.setTaxAmountSnapshot(totals.taxAmount());
        order.setTotalGrossSnapshot(totals.totalGross());
        order.setUpdatedBy(userId);

        OrderDomain updated = orderPersistence.update(order);

        List<OrderTaxDomain> orderTaxes = buildOrderTaxes(orderId, taxContext, totals.subtotalGross(), userId);
        orderPersistence.replaceOrderTaxes(orderId, orderTaxes);

        List<OrderItemTaxDomain> itemTaxes = buildOrderItemTaxes(items, taxContext, userId);
        orderPersistence.replaceOrderItemTaxes(orderId, itemTaxes);
        applyItemTaxes(items, itemTaxes);

        updated.setItems(items);
        updated.setTaxes(orderTaxes);
        return updated;
    }

    @Override
    @Transactional
    public OrderDomain updateItemQuantity(
            UUID restaurantId, UUID orderId, UUID orderItemId, BigDecimal quantity, UUID userId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        RestaurantDomain restaurant = loadRestaurant(order.getRestaurantId());
        TaxContext taxContext = resolveTaxContext(restaurant);

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        OrderItemDomain target = findItem(order, orderItemId);
        BigDecimal unitPrice = defaulted(target.getUnitPriceSnapshot());
        BigDecimal subtotal = unitPrice.multiply(quantity).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        target.setQuantity(quantity);
        target.setSubtotalGrossSnapshot(subtotal);

        orderPersistence.updateItemQuantity(orderItemId, quantity, subtotal, userId);

        Totals totals = computeTotals(order.getItems(), taxContext.rate());
        order.setTaxRateSnapshot(taxContext.rate());
        order.setSubtotalGrossSnapshot(totals.subtotalGross());
        order.setTaxAmountSnapshot(totals.taxAmount());
        order.setTotalGrossSnapshot(totals.totalGross());
        order.setUpdatedBy(userId);

        OrderDomain updated = orderPersistence.update(order);

        List<OrderTaxDomain> orderTaxes = buildOrderTaxes(orderId, taxContext, totals.subtotalGross(), userId);
        orderPersistence.replaceOrderTaxes(orderId, orderTaxes);

        List<OrderItemTaxDomain> itemTaxes = buildOrderItemTaxes(order.getItems(), taxContext, userId);
        orderPersistence.replaceOrderItemTaxes(orderId, itemTaxes);
        applyItemTaxes(order.getItems(), itemTaxes);

        updated.setItems(order.getItems());
        updated.setTaxes(orderTaxes);
        return updated;
    }

    @Override
    @Transactional
    public OrderDomain removeItem(UUID restaurantId, UUID orderId, UUID orderItemId, UUID userId) {
        OrderDomain order = loadOrder(restaurantId, orderId);
        RestaurantDomain restaurant = loadRestaurant(order.getRestaurantId());
        TaxContext taxContext = resolveTaxContext(restaurant);

        findItem(order, orderItemId);
        orderPersistence.deleteItem(orderItemId);

        List<OrderItemDomain> remaining = new ArrayList<>(order.getItems());
        remaining.removeIf(item -> orderItemId.equals(item.getId()));

        Totals totals = computeTotals(remaining, taxContext.rate());
        order.setItems(remaining);
        order.setTaxRateSnapshot(taxContext.rate());
        order.setSubtotalGrossSnapshot(totals.subtotalGross());
        order.setTaxAmountSnapshot(totals.taxAmount());
        order.setTotalGrossSnapshot(totals.totalGross());
        order.setUpdatedBy(userId);

        OrderDomain updated = orderPersistence.update(order);

        List<OrderTaxDomain> orderTaxes = buildOrderTaxes(orderId, taxContext, totals.subtotalGross(), userId);
        orderPersistence.replaceOrderTaxes(orderId, orderTaxes);

        List<OrderItemTaxDomain> itemTaxes = buildOrderItemTaxes(remaining, taxContext, userId);
        orderPersistence.replaceOrderItemTaxes(orderId, itemTaxes);
        applyItemTaxes(remaining, itemTaxes);

        updated.setItems(remaining);
        updated.setTaxes(orderTaxes);
        return updated;
    }

    @Override
    public java.util.Optional<OrderDomain> findById(UUID restaurantId, UUID orderId) {
        return orderPersistence.findByIdWithItems(orderId)
                .filter(order -> restaurantId.equals(order.getRestaurantId()));
    }

    @Override
    public PageResponse<OrderDomain> findAllPaged(UUID restaurantId, int page, int size, String search) {
        return orderPersistence.findAllPaged(restaurantId, page, size, search);
    }

    private RestaurantDomain loadRestaurant(UUID restaurantId) {
        return restaurantPersistence.findById(restaurantId)
                .orElseThrow(() -> RestaurantNotFoundException.forId(restaurantId));
    }

    private void validateTableContext(OrderDomain order) {
        if (order.getServiceType() == null) {
            throw new IllegalArgumentException("Order must include a service type.");
        }
        if (order.getServiceType() == ServiceType.DINE_IN) {
            if (order.getTableId() == null) {
                throw new IllegalArgumentException("Dine-in order must include a restaurant table.");
            }
            tableGateway.validateAvailableForOrder(order.getRestaurantId(), order.getTableId());
            return;
        }
        if (order.getTableId() != null) {
            throw new IllegalArgumentException("Only dine-in orders can include a restaurant table.");
        }
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

    private List<OrderItemDomain> buildItems(List<OrderItemDomain> items, UUID restaurantId, UUID userId) {
        List<OrderItemDomain> prepared = new ArrayList<>();
        for (OrderItemDomain item : items) {
            prepared.add(buildItem(item, restaurantId, userId));
        }
        return prepared;
    }

    private OrderItemDomain buildItem(OrderItemDomain item, UUID restaurantId, UUID userId) {
        if (item.getItemId() == null) {
            throw new IllegalArgumentException("Order item must include an itemId.");
        }

        ItemDomain sourceItem = itemPersistence.findById(item.getItemId())
                .orElseThrow(() -> ItemNotFoundException.forId(item.getItemId()));
        if (!restaurantId.equals(sourceItem.getRestaurantId())) {
            throw new IllegalArgumentException("Item does not belong to the restaurant.");
        }
        if (sourceItem.getItemClass() != ItemClass.PRODUCT) {
            throw new IllegalArgumentException("Only saleable products can be added to orders.");
        }

        validateSubmenuNode(item, sourceItem);

        BigDecimal quantity = normalizeQuantity(item.getQuantity());
        BigDecimal unitPrice = item.getUnitPriceSnapshot() != null
                ? item.getUnitPriceSnapshot()
                : defaulted(sourceItem.getSalePrice());
        BigDecimal subtotal = unitPrice.multiply(quantity).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return OrderItemDomain.builder()
                .orderId(item.getOrderId())
                .itemId(sourceItem.getId())
                .submenuNodeId(item.getSubmenuNodeId())
                .itemNameSnapshot(sourceItem.getName())
                .unitPriceSnapshot(unitPrice)
                .theoreticalCostSnapshot(defaulted(sourceItem.getTheoreticalCost()))
                .quantity(quantity)
                .subtotalGrossSnapshot(subtotal)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
    }

    private void validateSubmenuNode(OrderItemDomain item, ItemDomain sourceItem) {
        if (item.getSubmenuNodeId() == null) {
            return;
        }
        SubmenuNodeDomain node = submenuNodeQuery.findNodeById(item.getSubmenuNodeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Submenu node not found: " + item.getSubmenuNodeId()));
        if (node.nodeType() != SubmenuNodeType.PRODUCT) {
            throw new IllegalArgumentException("Submenu node is not a product node.");
        }
        if (node.itemId() != null && !node.itemId().equals(sourceItem.getId())) {
            throw new IllegalArgumentException("Submenu node does not match the item.");
        }
    }

    private Totals computeTotals(List<OrderItemDomain> items, BigDecimal rate) {
        BigDecimal subtotal = items.stream()
                .map(OrderItemDomain::getSubtotalGrossSnapshot)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
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

    private boolean resolvePrepaymentRequired(RestaurantDomain restaurant) {
        if (restaurant.getSettings() != null && restaurant.getSettings().prePaymentEnabled() != null) {
            return restaurant.getSettings().prePaymentEnabled();
        }
        return restaurant.getOperationMode() == OperationMode.PREPAID;
    }

    private List<OrderTaxDomain> buildOrderTaxes(UUID orderId, TaxContext taxContext,
            BigDecimal baseGross, UUID userId) {
        if (taxContext.definitions().isEmpty() || taxContext.rate().compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        List<OrderTaxDomain> taxes = new ArrayList<>();
        for (OrderTaxDefinition definition : taxContext.definitions()) {
            BigDecimal rate = normalizeRate(definition.rate());
            BigDecimal amount = calculateTaxFromGross(baseGross, rate);
            taxes.add(OrderTaxDomain.builder()
                    .orderId(orderId)
                    .taxId(definition.id())
                    .taxNameSnapshot(definition.name())
                    .taxRateSnapshot(rate)
                    .taxBaseSnapshot(baseGross)
                    .taxAmountSnapshot(amount)
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
                BigDecimal amount = calculateTaxFromGross(baseGross, rate);
                taxes.add(OrderItemTaxDomain.builder()
                        .orderItemId(item.getId())
                        .taxId(definition.id())
                        .taxNameSnapshot(definition.name())
                        .taxRateSnapshot(rate)
                        .taxBaseSnapshot(baseGross)
                        .taxAmountSnapshot(amount)
                        .createdBy(userId)
                        .build());
            }
        }
        return taxes;
    }

    private void applyItemTaxes(List<OrderItemDomain> items, List<OrderItemTaxDomain> taxes) {
        for (OrderItemDomain item : items) {
            List<OrderItemTaxDomain> itemTaxes = taxes.stream()
                    .filter(tax -> item.getId() != null && item.getId().equals(tax.getOrderItemId()))
                    .toList();
            item.setTaxes(itemTaxes);
        }
    }

    private BigDecimal normalizeQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        return quantity.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
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

    private record TaxContext(List<OrderTaxDefinition> definitions, BigDecimal rate) {
    }

    private record Totals(BigDecimal subtotalGross, BigDecimal taxAmount, BigDecimal totalGross) {
    }
}
