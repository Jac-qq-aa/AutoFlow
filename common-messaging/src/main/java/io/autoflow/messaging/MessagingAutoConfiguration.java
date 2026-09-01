package io.autoflow.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@EnableConfigurationProperties(MessagingProperties.class)
public class MessagingAutoConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "autoflow.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
    EventPublisher rocketMqEventPublisher(MessagingProperties properties, ObjectMapper objectMapper) throws Exception {
        return new RocketMqEventPublisher(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    EventPublisher loggingEventPublisher() {
        return new LoggingEventPublisher();
    }

    @Bean
    DeadLetterService deadLetterService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, EventPublisher publisher) {
        return new DeadLetterService(jdbcTemplate, objectMapper, publisher);
    }

    @Bean
    BusinessMetrics businessMetrics(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        return new BusinessMetrics(jdbcTemplate, meterRegistry);
    }
}
