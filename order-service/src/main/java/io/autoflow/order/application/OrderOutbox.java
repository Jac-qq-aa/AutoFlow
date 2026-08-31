package io.autoflow.order.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.autoflow.messaging.DomainEvent;
import io.autoflow.messaging.EventPublisher;
import io.autoflow.messaging.Topics;
import io.autoflow.order.persistence.OutboxEventEntity;
import io.autoflow.order.persistence.OutboxEventMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderOutbox {
    private static final Logger log = LoggerFactory.getLogger(OrderOutbox.class);
    private final OutboxEventMapper mapper;
    private final ObjectMapper objectMapper;
    private final EventPublisher publisher;

    public OrderOutbox(OutboxEventMapper mapper, ObjectMapper objectMapper, EventPublisher publisher) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.publisher = publisher;
    }

    public UUID append(String eventType, String aggregateId, Object payload) {
        try {
            var entity = new OutboxEventEntity();
            var eventId = UUID.randomUUID();
            entity.eventId = eventId.toString();
            entity.aggregateId = aggregateId;
            entity.eventType = eventType;
            entity.payload = objectMapper.writeValueAsString(payload);
            entity.status = "PENDING";
            entity.attempts = 0;
            entity.nextAttemptAt = LocalDateTime.now();
            entity.createdAt = LocalDateTime.now();
            mapper.insert(entity);
            return eventId;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not append outbox event", exception);
        }
    }

    @Scheduled(fixedDelayString = "${autoflow.outbox.interval-ms:1000}")
    public void publishPending() {
        for (var row : mapper.findPublishable()) {
            try {
                var event = new DomainEvent(UUID.fromString(row.eventId), row.eventType, row.aggregateId, row.createdAt.toInstant(java.time.ZoneOffset.UTC), objectMapper.readTree(row.payload));
                publisher.publish(Topics.ORDER_EVENTS, event);
                mapper.markSent(row.eventId);
            } catch (Exception exception) {
                mapper.scheduleRetry(row.eventId);
                log.warn("Order outbox publish failed eventId={} attempt={}", row.eventId, row.attempts + 1, exception);
            }
        }
    }
}

