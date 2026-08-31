package io.autoflow.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record DomainEvent(UUID eventId, String eventType, String aggregateId, Instant occurredAt, JsonNode payload) {
}

