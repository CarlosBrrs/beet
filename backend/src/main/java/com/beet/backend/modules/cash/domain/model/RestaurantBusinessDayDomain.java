package com.beet.backend.modules.cash.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
public class RestaurantBusinessDayDomain {
    private UUID id;
    private UUID restaurantId;
    private LocalDate businessDate;
    private String timeZoneSnapshot;
    private BusinessDayStatus status;
    private OffsetDateTime openedAt;
    private UUID openedBy;
    private OffsetDateTime closedAt;
    private UUID closedBy;
    private int openSessionCount;
    private int pendingOrderCount;
    private int missingReconciliationCount;
    private int unexplainedDifferenceCount;
    private List<BusinessDayClosureDomain> closures;
}
