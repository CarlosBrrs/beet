package com.beet.backend.modules.order.infrastructure.input.sse;

import com.beet.backend.modules.order.application.dto.KitchenTicketEventResponse;
import com.beet.backend.modules.order.domain.model.KitchenTicketDomain;
import com.beet.backend.modules.order.domain.spi.KitchenTicketEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class KitchenTicketSseHub implements KitchenTicketEventPort {
    private static final long TIMEOUT_MS = 0L;
    private static final String CREATED_EVENT = "kitchen.ticket.created";
    private static final String UPDATED_EVENT = "kitchen.ticket.updated";
    private static final String STATUS_CHANGED_EVENT = "kitchen.ticket.status_changed";
    private static final String HEARTBEAT_EVENT = "kitchen.heartbeat";

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID restaurantId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(restaurantId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(restaurantId, emitter));
        emitter.onTimeout(() -> remove(restaurantId, emitter));
        emitter.onError(error -> remove(restaurantId, emitter));
        send(restaurantId, emitter, HEARTBEAT_EVENT, Map.of("occurredAt", OffsetDateTime.now().toString()));
        return emitter;
    }

    @Override
    public void ticketCreated(KitchenTicketDomain ticket) {
        publishAfterCommit(CREATED_EVENT, ticket);
    }

    @Override
    public void ticketUpdated(KitchenTicketDomain ticket) {
        publishAfterCommit(UPDATED_EVENT, ticket);
    }

    @Override
    public void ticketStatusChanged(KitchenTicketDomain ticket) {
        publishAfterCommit(STATUS_CHANGED_EVENT, ticket);
    }

    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.forEach((restaurantId, restaurantEmitters) -> {
            for (SseEmitter emitter : restaurantEmitters) {
                send(restaurantId, emitter, HEARTBEAT_EVENT, Map.of("occurredAt", OffsetDateTime.now().toString()));
            }
        });
    }

    private void publishAfterCommit(String eventType, KitchenTicketDomain ticket) {
        if (ticket == null || ticket.getRestaurantId() == null) {
            return;
        }
        Runnable publisher = () -> publish(eventType, ticket);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.run();
                }
            });
            return;
        }
        publisher.run();
    }

    private void publish(String eventType, KitchenTicketDomain ticket) {
        KitchenTicketEventResponse event = new KitchenTicketEventResponse(
                eventType,
                ticket.getRestaurantId(),
                ticket.getId(),
                ticket.getOrderId(),
                ticket.getOrderDisplayCode(),
                ticket.getCustomerName(),
                ticket.getStatus(),
                OffsetDateTime.now());
        List<SseEmitter> restaurantEmitters = emitters.getOrDefault(ticket.getRestaurantId(), new CopyOnWriteArrayList<>());
        for (SseEmitter emitter : restaurantEmitters) {
            send(ticket.getRestaurantId(), emitter, eventType, event);
        }
    }

    private void send(UUID restaurantId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException exception) {
            log.debug("Removing stale KDS SSE emitter for restaurant {}", restaurantId, exception);
            remove(restaurantId, emitter);
        }
    }

    private void remove(UUID restaurantId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> restaurantEmitters = emitters.get(restaurantId);
        if (restaurantEmitters == null) {
            return;
        }
        restaurantEmitters.remove(emitter);
        if (restaurantEmitters.isEmpty()) {
            emitters.remove(restaurantId);
        }
    }
}