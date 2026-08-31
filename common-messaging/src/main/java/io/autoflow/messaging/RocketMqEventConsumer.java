package io.autoflow.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RocketMqEventConsumer implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(RocketMqEventConsumer.class);
    private final PushConsumer consumer;

    public RocketMqEventConsumer(MessagingProperties properties, ObjectMapper objectMapper, String group,
                                 Consumer<DomainEvent> handler, DeadLetterRecorder deadLetterRecorder, String... topics) throws Exception {
        var provider = ClientServiceProvider.loadService();
        var configuration = ClientConfiguration.newBuilder().setEndpoints(properties.endpoints()).build();
        Map<String, FilterExpression> subscriptions = Arrays.stream(topics)
            .collect(Collectors.toUnmodifiableMap(topic -> topic, topic -> FilterExpression.SUB_ALL));
        this.consumer = provider.newPushConsumerBuilder()
            .setClientConfiguration(configuration)
            .setConsumerGroup(group)
            .setSubscriptionExpressions(subscriptions)
            .setMessageListener(message -> {
                DomainEvent event = null;
                try {
                    var buffer = message.getBody().duplicate();
                    var bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    event = objectMapper.readValue(new String(bytes, StandardCharsets.UTF_8), DomainEvent.class);
                    handler.accept(event);
                    return ConsumeResult.SUCCESS;
                } catch (Exception exception) {
                    log.error("Event consumption failed topic={} messageId={} attempt={}", message.getTopic(), message.getMessageId(), message.getDeliveryAttempt(), exception);
                    if (event != null && message.getDeliveryAttempt() >= properties.maxDeliveryAttempts()) {
                        deadLetterRecorder.record(message.getTopic(), event, exception.toString());
                        return ConsumeResult.SUCCESS;
                    }
                    return ConsumeResult.FAILURE;
                }
            })
            .build();
    }

    @Override
    public void close() throws Exception {
        consumer.close();
    }
}
