package io.autoflow.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DeadLetterServiceTest {
    @Test
    void replayPublishesOriginalEventAndAuditsTheOperator() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var publisher = mock(EventPublisher.class);
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var event = new DomainEvent(UUID.randomUUID(), "PaymentSucceeded", "order-1", Instant.now(), mapper.createObjectNode());
        when(jdbc.queryForMap(any(String.class), eq("dlq-1"))).thenReturn(Map.of(
            "topic", "autoflow-fulfillment-events", "event_json", mapper.writeValueAsString(event), "status", "PENDING"));

        new DeadLetterService(jdbc, mapper, publisher).replay("dlq-1", "admin");

        verify(publisher).publish("autoflow-fulfillment-events", event);
        verify(jdbc).update(any(String.class), eq("admin"), eq("dlq-1"));
    }
}
