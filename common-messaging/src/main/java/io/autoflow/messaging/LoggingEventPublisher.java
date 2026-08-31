package io.autoflow.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingEventPublisher implements EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(String topic, DomainEvent event) {
        log.info("Messaging disabled; leaving event visible in logs. topic={} eventId={} type={}", topic, event.eventId(), event.eventType());
    }
}

