package com.beet.backend.modules.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class KitchenTicketDomain {
    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyy");

    private UUID id;
    private UUID restaurantId;
    private UUID orderId;
    private LocalDate orderBusinessDate;
    private Integer orderDailySequence;
    private String orderPublicCode;
    private KitchenTicketStatus status;
    private OffsetDateTime sentAt;
    private UUID sentBy;
    private OffsetDateTime startedAt;
    private UUID startedBy;
    private OffsetDateTime readyAt;
    private UUID readyBy;
    private OffsetDateTime canceledAt;
    private UUID canceledBy;
    private String notes;

    @Builder.Default
    private List<KitchenTicketLineDomain> lines = new ArrayList<>();

    public String getOrderNumber() {
        if (orderBusinessDate == null || orderDailySequence == null) {
            return null;
        }
        return ORDER_NUMBER_DATE_FORMAT.format(orderBusinessDate) + "-" + String.format("%04d", orderDailySequence);
    }

    public String getOrderDisplayCode() {
        String orderNumber = getOrderNumber();
        if (orderNumber == null || orderPublicCode == null || orderPublicCode.isBlank()) {
            return orderNumber;
        }
        return orderNumber + "/" + orderPublicCode;
    }
}
