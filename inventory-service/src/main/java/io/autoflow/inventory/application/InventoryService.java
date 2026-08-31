package io.autoflow.inventory.application;

import io.autoflow.common.error.BusinessException;
import io.autoflow.inventory.persistence.InventoryQuotaEntity;
import io.autoflow.inventory.persistence.InventoryQuotaMapper;
import io.autoflow.inventory.persistence.ReservationEntity;
import io.autoflow.inventory.persistence.ReservationMapper;
import io.autoflow.inventory.persistence.VehicleMapper;
import io.autoflow.messaging.EventTypes;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class InventoryService {
    private final InventoryQuotaMapper quotaMapper;
    private final ReservationMapper reservationMapper;
    private final VehicleMapper vehicleMapper;
    private final InventoryOutbox outbox;
    private final RedissonClient redisson;
    private final TransactionTemplate transactionTemplate;

    public InventoryService(InventoryQuotaMapper quotaMapper, ReservationMapper reservationMapper, VehicleMapper vehicleMapper,
                            InventoryOutbox outbox, RedissonClient redisson, TransactionTemplate transactionTemplate) {
        this.quotaMapper = quotaMapper; this.reservationMapper = reservationMapper; this.vehicleMapper = vehicleMapper;
        this.outbox = outbox; this.redisson = redisson; this.transactionTemplate = transactionTemplate;
    }

    public ReservationEntity reserve(String orderId, String storeId, String modelCode) {
        var existing = reservationMapper.findByOrderId(orderId);
        if (existing != null) return existing;
        var lock = redisson.getLock("inventory:" + storeId + ":" + modelCode);
        var acquired = false;
        try {
            acquired = lock.tryLock(2, TimeUnit.SECONDS);
            if (!acquired) throw new BusinessException("INVENTORY_BUSY", "Inventory is busy; retry later");
            var reservation = transactionTemplate.execute(status -> reserveInTransaction(orderId, storeId, modelCode));
            if (reservation == null) throw new BusinessException("OUT_OF_STOCK", "No sellable quota is available");
            return reservation;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("INVENTORY_INTERRUPTED", "Inventory reservation was interrupted");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private ReservationEntity reserveInTransaction(String orderId, String storeId, String modelCode) {
        try {
            var existing = reservationMapper.findByOrderId(orderId);
            if (existing != null) return existing;
            if (quotaMapper.reserve(storeId, modelCode) != 1) {
                outbox.append(EventTypes.INVENTORY_REJECTED, orderId, Map.of("orderId", orderId, "reason", "OUT_OF_STOCK"));
                return null;
            }
            var reservation = new ReservationEntity();
            reservation.reservationId = UUID.randomUUID().toString(); reservation.orderId = orderId;
            reservation.storeId = storeId; reservation.modelCode = modelCode; reservation.status = "RESERVED";
            reservation.createdAt = LocalDateTime.now(); reservation.updatedAt = reservation.createdAt;
            reservationMapper.insert(reservation);
            outbox.append(EventTypes.INVENTORY_RESERVED, orderId, Map.of("orderId", orderId, "reservationId", reservation.reservationId));
            return reservation;
        } catch (DuplicateKeyException duplicate) {
            return reservationMapper.findByOrderId(orderId);
        }
    }

    public String allocateVin(String orderId) {
        return transactionTemplate.execute(status -> {
            var reservation = requireReservation(orderId);
            if (reservation.vin != null) return reservation.vin;
            var vehicle = vehicleMapper.lockOneAvailable(reservation.storeId, reservation.modelCode);
            if (vehicle == null) throw new BusinessException("VIN_NOT_AVAILABLE", "No physical vehicle is available for allocation");
            if (vehicleMapper.allocate(vehicle.vin, orderId) != 1 || reservationMapper.markVinAllocated(orderId, vehicle.vin) != 1) {
                throw new BusinessException("VIN_ALLOCATION_RACE", "Vehicle allocation raced with another transaction");
            }
            outbox.append(EventTypes.VIN_ALLOCATED, orderId, Map.of("orderId", orderId, "vin", vehicle.vin));
            return vehicle.vin;
        });
    }

    public void release(String orderId) {
        transactionTemplate.executeWithoutResult(status -> {
            var reservation = reservationMapper.findByOrderId(orderId);
            if (reservation == null || "RELEASED".equals(reservation.status)) return;
            vehicleMapper.releaseByOrderId(orderId);
            if (reservationMapper.markReleased(orderId) == 1) {
                quotaMapper.release(reservation.storeId, reservation.modelCode);
                outbox.append(EventTypes.INVENTORY_RELEASED, orderId, Map.of("orderId", orderId));
            }
        });
    }

    public InventoryQuotaEntity quota(String storeId, String modelCode) {
        var quota = quotaMapper.find(storeId, modelCode);
        if (quota == null) throw new BusinessException("QUOTA_NOT_FOUND", "No quota exists for this store and model");
        return quota;
    }

    private ReservationEntity requireReservation(String orderId) {
        var reservation = reservationMapper.findByOrderId(orderId);
        if (reservation == null || "RELEASED".equals(reservation.status)) throw new BusinessException("RESERVATION_NOT_FOUND", "Active reservation not found");
        return reservation;
    }
}
