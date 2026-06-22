package com.beet.backend.modules.order.infrastructure.input.scheduling;

import com.beet.backend.modules.order.domain.api.OrderServicePort;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class PrepaidOrderExpirationScheduler {

    private static final int BATCH_SIZE = 100;

    private final OrderServicePort orderService;

    @Scheduled(fixedDelayString = "${beet.orders.prepaid-expiration-delay-ms:60000}")
    public void processExpiredOrders() {
        orderService.processExpiredAwaitingPayments(
                OffsetDateTime.now(ZoneOffset.UTC),
                BATCH_SIZE);
    }
}
