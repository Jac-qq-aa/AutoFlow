package io.autoflow.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.autoflow.inventory.application.InventoryEventHandler;
import io.autoflow.messaging.MessagingProperties;
import io.autoflow.messaging.DeadLetterService;
import io.autoflow.messaging.RocketMqEventConsumer;
import io.autoflow.messaging.Topics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryConsumerConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "autoflow.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
    RocketMqEventConsumer inventoryEventConsumer(MessagingProperties properties, ObjectMapper objectMapper, InventoryEventHandler handler, DeadLetterService deadLetters) throws Exception {
        return new RocketMqEventConsumer(properties, objectMapper, "autoflow-inventory-service", handler::handle, deadLetters,
            Topics.ORDER_EVENTS, Topics.FULFILLMENT_EVENTS);
    }
}
