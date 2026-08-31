package io.autoflow.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.producer.Producer;

public final class RocketMqEventPublisher implements EventPublisher, AutoCloseable {
    private final ClientServiceProvider provider;
    private final Producer producer;
    private final ObjectMapper objectMapper;

    public RocketMqEventPublisher(MessagingProperties properties, ObjectMapper objectMapper) throws ClientException {
        this.provider = ClientServiceProvider.loadService();
        this.objectMapper = objectMapper;
        var configuration = ClientConfiguration.newBuilder().setEndpoints(properties.endpoints()).build();
        this.producer = provider.newProducerBuilder()
            .setClientConfiguration(configuration)
            .setTopics(Topics.ORDER_EVENTS, Topics.INVENTORY_EVENTS, Topics.FULFILLMENT_EVENTS)
            .build();
    }

    @Override
    public void publish(String topic, DomainEvent event) throws Exception {
        var message = provider.newMessageBuilder()
            .setTopic(topic)
            .setKeys(event.eventId().toString())
            .setMessageGroup(event.aggregateId())
            .setTag(event.eventType())
            .setBody(objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8))
            .build();
        producer.send(message);
    }

    @Override
    public void close() throws Exception {
        producer.close();
    }
}

