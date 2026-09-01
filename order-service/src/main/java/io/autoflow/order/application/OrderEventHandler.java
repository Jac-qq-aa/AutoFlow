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
    private final SalesOrderMapper orders; private final OrderOutbox outbox; private final CacheManager cacheManager;
    public OrderEventHandler(JdbcTemplate jdbc, TransactionTemplate transactions, SalesOrderMapper orders,
                             OrderOutbox outbox, CacheManager cacheManager) {
        this.jdbc = jdbc; this.transactions = transactions; this.orders = orders; this.outbox = outbox; this.cacheManager = cacheManager;
    }

    public void handle(DomainEvent event) {
        transactions.executeWithoutResult(status -> {
            if (!claim(event)) return;
            switch (event.eventType()) {
                case EventTypes.INVENTORY_RESERVED -> orders.inventoryReserved(event.aggregateId());
                case EventTypes.INVENTORY_REJECTED -> orders.inventoryRejected(event.aggregateId());
                case EventTypes.PAYMENT_SUCCEEDED -> paymentSucceeded(event);
                case EventTypes.PAYMENT_FAILED -> paymentFailed(event);
                case EventTypes.VIN_ALLOCATED -> {
                    orders.recordVinAllocated(event.aggregateId(), event.payload().path("vin").asText());
                    orders.reconcileFulfillmentState(event.aggregateId());
                }
                case EventTypes.INVENTORY_RELEASED -> { orders.inventoryReleased(event.aggregateId()); orders.finalizeCancellation(event.aggregateId()); }
                case EventTypes.REFUND_SUCCEEDED -> { orders.refundSucceeded(event.aggregateId()); orders.finalizeCancellation(event.aggregateId()); }
                case EventTypes.DELIVERY_COMPLETED -> {
                    orders.recordDeliveryCompleted(event.aggregateId());
                    orders.reconcileFulfillmentState(event.aggregateId());
                }
                default -> { return; }
            }
            var cache = cacheManager.getCache("orders");
            if (cache != null) cache.clear();
        });
    }

    private void paymentSucceeded(DomainEvent event) {
        var changed = orders.recordPaymentSucceeded(event.aggregateId());
        var order = orders.selectById(event.aggregateId());
        if (changed == 1 && order != null && "REFUNDING".equals(order.status)) {
            outbox.append(EventTypes.REFUND_REQUESTED, event.aggregateId(),
                java.util.Map.of("orderId", event.aggregateId(), "amount", order.amount));
        }
        orders.reconcileFulfillmentState(event.aggregateId());
    }

    private void paymentFailed(DomainEvent event) {
        orders.recordPaymentFailed(event.aggregateId());
        orders.finalizeCancellation(event.aggregateId());
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
