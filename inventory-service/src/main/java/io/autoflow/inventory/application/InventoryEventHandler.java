package io.autoflow.inventory.application;

import io.autoflow.inventory.persistence.InventoryQuotaMapper;
import io.autoflow.inventory.persistence.ReservationEntity;
import io.autoflow.inventory.persistence.ReservationMapper;
import io.autoflow.inventory.persistence.VehicleMapper;
import io.autoflow.messaging.DomainEvent;
import io.autoflow.messaging.EventTypes;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class InventoryEventHandler {
    private final JdbcTemplate jdbc; private final TransactionTemplate transactions; private final RedissonClient redisson;
    private final InventoryQuotaMapper quotas; private final ReservationMapper reservations;
    private final VehicleMapper vehicles; private final InventoryOutbox outbox;

    public InventoryEventHandler(JdbcTemplate jdbc, TransactionTemplate transactions, RedissonClient redisson,
                                 InventoryQuotaMapper quotas, ReservationMapper reservations, VehicleMapper vehicles, InventoryOutbox outbox) {
        this.jdbc = jdbc; this.transactions = transactions; this.redisson = redisson; this.quotas = quotas;
        this.reservations = reservations; this.vehicles = vehicles; this.outbox = outbox;
    }

    public void handle(DomainEvent event) {
        switch (event.eventType()) {
            case EventTypes.INVENTORY_RESERVATION_REQUESTED -> reserve(event);
            case EventTypes.PAYMENT_SUCCEEDED -> allocateVin(event);
            case EventTypes.ORDER_CANCELLATION_REQUESTED -> release(event);
            default -> { }
        }
    }

    private void reserve(DomainEvent event) {
        var storeId = event.payload().path("storeId").asText();
        var modelCode = event.payload().path("modelCode").asText();
        var lock = redisson.getLock("inventory:" + storeId + ":" + modelCode);
        var acquired = false;
        try {
            acquired = lock.tryLock(2, TimeUnit.SECONDS);
            if (!acquired) throw new IllegalStateException("Inventory lock timed out; RocketMQ will retry");
            transactions.executeWithoutResult(status -> {
                if (!claim(event)) return;
                var existing = reservations.findByOrderId(event.aggregateId());
                if (existing != null) return;
                if (quotas.reserve(storeId, modelCode) != 1) {
                    outbox.append(EventTypes.INVENTORY_REJECTED, event.aggregateId(), Map.of("orderId", event.aggregateId(), "reason", "OUT_OF_STOCK"));
                    return;
                }
                var reservation = new ReservationEntity(); reservation.reservationId = UUID.randomUUID().toString();
                reservation.orderId = event.aggregateId(); reservation.storeId = storeId; reservation.modelCode = modelCode;
                reservation.status = "RESERVED"; reservation.createdAt = LocalDateTime.now(); reservation.updatedAt = reservation.createdAt;
                reservations.insert(reservation);
                outbox.append(EventTypes.INVENTORY_RESERVED, event.aggregateId(), Map.of("orderId", event.aggregateId(), "reservationId", reservation.reservationId));
            });
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Inventory lock interrupted", interrupted);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private void allocateVin(DomainEvent event) {
        transactions.executeWithoutResult(status -> {
            if (!claim(event)) return;
            var reservation = reservations.findByOrderId(event.aggregateId());
            if (reservation == null || "RELEASED".equals(reservation.status) || reservation.vin != null) return;
            var vehicle = vehicles.lockOneAvailable(reservation.storeId, reservation.modelCode);
            if (vehicle == null) throw new IllegalStateException("Reserved quota has no physical VIN; retry or compensate");
            if (vehicles.allocate(vehicle.vin, event.aggregateId()) != 1 || reservations.markVinAllocated(event.aggregateId(), vehicle.vin) != 1) {
                throw new IllegalStateException("VIN allocation raced");
            }
            outbox.append(EventTypes.VIN_ALLOCATED, event.aggregateId(), Map.of("orderId", event.aggregateId(), "vin", vehicle.vin));
        });
    }

    private void release(DomainEvent event) {
        transactions.executeWithoutResult(status -> {
            if (!claim(event)) return;
            var reservation = reservations.findByOrderId(event.aggregateId());
            if (reservation == null || "RELEASED".equals(reservation.status)) {
                outbox.append(EventTypes.INVENTORY_RELEASED, event.aggregateId(), Map.of("orderId", event.aggregateId()));
                return;
            }
            vehicles.releaseByOrderId(event.aggregateId());
            if (reservations.markReleased(event.aggregateId()) == 1) {
                quotas.release(reservation.storeId, reservation.modelCode);
                outbox.append(EventTypes.INVENTORY_RELEASED, event.aggregateId(), Map.of("orderId", event.aggregateId()));
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

