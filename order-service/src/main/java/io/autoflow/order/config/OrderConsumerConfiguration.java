package io.autoflow.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.autoflow.messaging.MessagingProperties;
import io.autoflow.messaging.DeadLetterService;
import io.autoflow.messaging.RocketMqEventConsumer;
import io.autoflow.messaging.Topics;
import io.autoflow.order.application.OrderEventHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderConsumerConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "autoflow.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
    RocketMqEventConsumer orderEventConsumer(MessagingProperties properties, ObjectMapper objectMapper, OrderEventHandler handler, DeadLetterService deadLetters) throws Exception {
        return new RocketMqEventConsumer(properties, objectMapper, "autoflow-order-service", handler::handle, deadLetters,
            Topics.INVENTORY_EVENTS, Topics.FULFILLMENT_EVENTS);
    }
}
