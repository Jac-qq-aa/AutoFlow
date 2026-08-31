package io.autoflow.fulfillment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.autoflow.fulfillment.application.FulfillmentEventHandler;
import io.autoflow.messaging.MessagingProperties;
import io.autoflow.messaging.DeadLetterService;
import io.autoflow.messaging.RocketMqEventConsumer;
import io.autoflow.messaging.Topics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FulfillmentConsumerConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "autoflow.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
    RocketMqEventConsumer fulfillmentEventConsumer(MessagingProperties properties, ObjectMapper objectMapper, FulfillmentEventHandler handler, DeadLetterService deadLetters) throws Exception {
        return new RocketMqEventConsumer(properties, objectMapper, "autoflow-fulfillment-service", handler::handle, deadLetters,
            Topics.ORDER_EVENTS, Topics.INVENTORY_EVENTS);
    }
}
