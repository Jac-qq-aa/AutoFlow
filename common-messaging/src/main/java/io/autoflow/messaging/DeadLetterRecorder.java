package io.autoflow.messaging;

public interface DeadLetterRecorder {
    void record(String topic, DomainEvent event, String reason);
}

