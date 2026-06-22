package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.beet.backend.shared.domain.model.OperationMode;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderDomain {
    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyy");

    private UUID id;
    private UUID restaurantId;
    private UUID businessDayId;
    private UUID cashSessionId;
    private UUID originCashSessionId;
    private UUID originDeviceId;
    private LocalDate businessDate;
    private Integer dailySequence;
    private String publicCode;
    private OrderStatus orderStatus;
    private KitchenStatus kitchenStatus;
    private PaymentStatus paymentStatus;
    private ServiceType serviceType;
    private OperationMode operationModeSnapshot;
    private UUID tableId;
    private String customerName;
    private String customerPhone;
    private String deliveryContactName;
    private String deliveryPhone;
    private String deliveryAddress;
    private String deliveryNotes;
    private BigDecimal deliveryFee;
    private DeliveryStatus deliveryStatus;
    private boolean prepaymentRequiredSnapshot;
    private BigDecimal taxRateSnapshot;
    private BigDecimal subtotalGrossSnapshot;
    private BigDecimal taxAmountSnapshot;
    private BigDecimal totalGrossSnapshot;
    private BigDecimal tipTotalSnapshot;
    @Builder.Default
    private BigDecimal refundDueSnapshot = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal refundedTotalSnapshot = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal paidTotal = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal remainingBalance = BigDecimal.ZERO;
    private OffsetDateTime paymentExpiresAt;
    private OffsetDateTime paymentExpiredAt;
    private OffsetDateTime expirationProcessedAt;
    private Integer prepaidOrderExpirationMinutes;
    private String notes;
    private OffsetDateTime completedAt;
    private OffsetDateTime canceledAt;
    private String cancelReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    @Builder.Default
    private List<OrderItemDomain> items = new ArrayList<>();

    @Builder.Default
    private List<OrderTaxDomain> taxes = new ArrayList<>();

    @Builder.Default
    private List<KitchenTicketDomain> kitchenTickets = new ArrayList<>();

    @Builder.Default
    private List<PaymentDomain> payments = new ArrayList<>();

    @Builder.Default
    private List<PaymentRefundDomain> refunds = new ArrayList<>();

    public String getOrderNumber() {
        if (businessDate == null || dailySequence == null) {
            return null;
        }
        return ORDER_NUMBER_DATE_FORMAT.format(businessDate) + "-" + String.format("%04d", dailySequence);
    }

    public String getDisplayCode() {
        String orderNumber = getOrderNumber();
        if (orderNumber == null || publicCode == null || publicCode.isBlank()) {
            return orderNumber;
        }
        return orderNumber + "/" + publicCode;
    }

    public boolean isPaymentExpired() {
        return paymentExpiredAt != null;
    }
}
