package io.autoflow.fulfillment.application;

import io.autoflow.messaging.DomainEvent;
import io.autoflow.messaging.EventTypes;
import java.sql.Timestamp;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class FulfillmentEventHandler {
    private final JdbcTemplate jdbc; private final TransactionTemplate transactions; private final FulfillmentService service;
    public FulfillmentEventHandler(JdbcTemplate jdbc, TransactionTemplate transactions, FulfillmentService service) {
        this.jdbc = jdbc; this.transactions = transactions; this.service = service;
    }

    public void handle(DomainEvent event) {
        transactions.executeWithoutResult(status -> {
            if (!claim(event)) return;
            switch (event.eventType()) {
                case EventTypes.PAYMENT_REQUESTED -> service.simulatePayment(event.aggregateId(), event.payload().path("amount").decimalValue(), event.payload().path("scenario").asText("SUCCESS"));
                case EventTypes.REFUND_REQUESTED -> service.refund(event.aggregateId(), event.payload().path("amount").decimalValue());
                case EventTypes.VIN_ALLOCATED -> service.createDelivery(event.aggregateId(), event.payload().path("vin").asText());
                default -> { }
            }
        });
    }

    private boolean claim(DomainEvent event) {
        try {
            return jdbc.update("INSERT INTO processed_event(event_id,event_type,processed_at) VALUES (?,?,?)",
                event.eventId().toString(), event.eventType(), Timestamp.from(java.time.Instant.now())) == 1;
        } catch (DuplicateKeyException duplicate) { return false; }
    }
}

