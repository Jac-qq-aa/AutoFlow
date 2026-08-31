package io.autoflow.order.application;

import io.autoflow.messaging.DomainEvent;
import io.autoflow.messaging.EventTypes;
import io.autoflow.order.persistence.SalesOrderMapper;
import java.sql.Timestamp;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OrderEventHandler {
    private final JdbcTemplate jdbc; private final TransactionTemplate transactions;
    private final SalesOrderMapper orders; private final CacheManager cacheManager;
    public OrderEventHandler(JdbcTemplate jdbc, TransactionTemplate transactions, SalesOrderMapper orders, CacheManager cacheManager) {
        this.jdbc = jdbc; this.transactions = transactions; this.orders = orders; this.cacheManager = cacheManager;
    }

    public void handle(DomainEvent event) {
        transactions.executeWithoutResult(status -> {
            if (!claim(event)) return;
            switch (event.eventType()) {
                case EventTypes.INVENTORY_RESERVED -> orders.inventoryReserved(event.aggregateId());
                case EventTypes.INVENTORY_REJECTED -> orders.inventoryRejected(event.aggregateId());
                case EventTypes.PAYMENT_SUCCEEDED -> orders.paymentSucceeded(event.aggregateId());
                case EventTypes.PAYMENT_FAILED -> orders.paymentFailed(event.aggregateId());
                case EventTypes.VIN_ALLOCATED -> orders.allocateVin(event.aggregateId(), event.payload().path("vin").asText());
                case EventTypes.INVENTORY_RELEASED -> { orders.inventoryReleased(event.aggregateId()); orders.finalizeCancellation(event.aggregateId()); }
                case EventTypes.REFUND_SUCCEEDED -> { orders.refundSucceeded(event.aggregateId()); orders.finalizeCancellation(event.aggregateId()); }
                case EventTypes.DELIVERY_COMPLETED -> orders.deliveryCompleted(event.aggregateId());
                default -> { return; }
            }
            var cache = cacheManager.getCache("orders");
            if (cache != null) cache.evict(event.aggregateId());
        });
    }

    private boolean claim(DomainEvent event) {
        try {
            return jdbc.update("INSERT INTO processed_event(event_id,event_type,processed_at) VALUES (?,?,?)",
                event.eventId().toString(), event.eventType(), Timestamp.from(java.time.Instant.now())) == 1;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }
}

