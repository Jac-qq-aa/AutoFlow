package io.autoflow.order.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.autoflow.messaging.EventPublisher;
import io.autoflow.order.persistence.OutboxEventEntity;
import io.autoflow.order.persistence.OutboxEventMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderOutboxTest {
    @Test
    void brokerFailureSchedulesExponentialRetryInsteadOfLosingTheEvent() throws Exception {
        var mapper = mock(OutboxEventMapper.class);
        var publisher = mock(EventPublisher.class);
        var row = new OutboxEventEntity();
        row.eventId = UUID.randomUUID().toString(); row.aggregateId = "order-1"; row.eventType = "OrderCreated";
        row.payload = "{}"; row.attempts = 0; row.createdAt = LocalDateTime.now();
        when(mapper.findPublishable()).thenReturn(List.of(row));
        doThrow(new IllegalStateException("broker unavailable")).when(publisher).publish(any(), any());

        new OrderOutbox(mapper, new ObjectMapper(), publisher).publishPending();

        verify(mapper).scheduleRetry(row.eventId);
    }
}
