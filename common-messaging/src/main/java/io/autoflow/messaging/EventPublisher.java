package io.autoflow.messaging;

public interface EventPublisher {
    void publish(String topic, DomainEvent event) throws Exception;
}

