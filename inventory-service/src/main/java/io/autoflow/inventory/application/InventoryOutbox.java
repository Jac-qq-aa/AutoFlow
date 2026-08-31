package io.autoflow.inventory.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.autoflow.inventory.persistence.OutboxEventEntity;
import io.autoflow.inventory.persistence.OutboxEventMapper;
import io.autoflow.messaging.DomainEvent;
import io.autoflow.messaging.EventPublisher;
import io.autoflow.messaging.Topics;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryOutbox {
    private static final Logger log = LoggerFactory.getLogger(InventoryOutbox.class);
    private final OutboxEventMapper mapper;
    private final ObjectMapper objectMapper;
    private final EventPublisher publisher;

    public InventoryOutbox(OutboxEventMapper mapper, ObjectMapper objectMapper, EventPublisher publisher) {
        this.mapper = mapper; this.objectMapper = objectMapper; this.publisher = publisher;
    }

    public void append(String type, String aggregateId, Object payload) {
        try {
            var row = new OutboxEventEntity();
            row.eventId = UUID.randomUUID().toString(); row.aggregateId = aggregateId; row.eventType = type;
            row.payload = objectMapper.writeValueAsString(payload); row.status = "PENDING"; row.attempts = 0;
            row.nextAttemptAt = LocalDateTime.now(); row.createdAt = row.nextAttemptAt; mapper.insert(row);
        } catch (Exception exception) { throw new IllegalStateException("Could not append inventory outbox event", exception); }
    }

    @Scheduled(fixedDelayString = "${autoflow.outbox.interval-ms:1000}")
    public void publishPending() {
        for (var row : mapper.findPublishable()) {
            try {
                var event = new DomainEvent(UUID.fromString(row.eventId), row.eventType, row.aggregateId,
                    row.createdAt.toInstant(ZoneOffset.UTC), objectMapper.readTree(row.payload));
                publisher.publish(Topics.INVENTORY_EVENTS, event); mapper.markSent(row.eventId);
            } catch (Exception exception) {
                mapper.scheduleRetry(row.eventId); log.warn("Inventory outbox publish failed eventId={}", row.eventId, exception);
            }
        }
    }
}

