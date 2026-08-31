package io.autoflow.order.application;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.autoflow.common.error.BusinessException;
import io.autoflow.messaging.EventTypes;
import io.autoflow.order.domain.OrderStateMachine;
import io.autoflow.order.domain.OrderStatus;
import io.autoflow.order.persistence.SalesOrderEntity;
import io.autoflow.order.persistence.SalesOrderMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final SalesOrderMapper mapper;
    private final OrderOutbox outbox;
    private final OrderStateMachine stateMachine = new OrderStateMachine();

    public OrderService(SalesOrderMapper mapper, OrderOutbox outbox) {
        this.mapper = mapper;
        this.outbox = outbox;
    }

    @Transactional
    public SalesOrderEntity create(CreateOrderCommand command, RequestUser user) {
        user.requireAnyRole("SALES", "STORE_MANAGER", "ADMIN");
        if (!user.isAdmin() && !user.storeId().equals(command.storeId())) {
            throw new BusinessException("STORE_ACCESS_DENIED", "Sales users can only create orders for their own store");
        }
        var entity = new SalesOrderEntity();
        entity.orderId = UUID.randomUUID().toString();
        entity.orderNo = "AF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ThreadLocalRandom.current().nextInt(1000, 9999);
        entity.channel = command.channel();
        entity.channelOrderNo = command.channelOrderNo() == null || command.channelOrderNo().isBlank() ? null : command.channelOrderNo();
        entity.storeId = command.storeId();
        entity.customerName = command.customerName();
        entity.customerPhone = command.customerPhone();
        entity.modelCode = command.modelCode();
        entity.amount = command.amount();
        entity.status = OrderStatus.PENDING_REVIEW.name();
        entity.inventoryStatus = "NOT_RESERVED";
        entity.paymentStatus = "UNPAID";
        entity.fulfillmentStatus = "NOT_STARTED";
        entity.version = 0;
        entity.createdBy = user.userId();
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = entity.createdAt;
        mapper.insert(entity);
        return entity;
    }

    @Transactional
    @CacheEvict(cacheNames = "orders", key = "#orderId")
    public SalesOrderEntity approve(String orderId, RequestUser user) {
        user.requireAnyRole("STORE_MANAGER", "ADMIN");
        var order = requireAccessible(orderId, user);
        stateMachine.requireApprovalAllowed(OrderStatus.valueOf(order.status));
        ensureUpdated(mapper.transition(orderId, order.status, OrderStatus.PENDING_STOCK.name()));
        outbox.append(EventTypes.INVENTORY_RESERVATION_REQUESTED, orderId,
            Map.of("orderId", orderId, "storeId", order.storeId, "modelCode", order.modelCode));
        return require(orderId);
    }

    @Transactional
    @CacheEvict(cacheNames = "orders", key = "#orderId")
    public SalesOrderEntity requestPayment(String orderId, RequestUser user, String scenario) {
        var order = requireAccessible(orderId, user);
        stateMachine.requirePaymentAllowed(OrderStatus.valueOf(order.status));
        ensureUpdated(mapper.markPaymentProcessing(orderId));
        outbox.append(EventTypes.PAYMENT_REQUESTED, orderId,
            Map.of("orderId", orderId, "amount", order.amount, "scenario", scenario == null ? "SUCCESS" : scenario));
        return require(orderId);
    }

    @Transactional
    @CacheEvict(cacheNames = "orders", key = "#orderId")
    public SalesOrderEntity cancel(String orderId, RequestUser user, String reason) {
        var order = requireAccessible(orderId, user);
        stateMachine.requireCancellationAllowed(OrderStatus.valueOf(order.status));
        ensureUpdated(mapper.transition(orderId, order.status, OrderStatus.CANCELLING.name()));
        outbox.append(EventTypes.ORDER_CANCELLATION_REQUESTED, orderId,
            Map.of("orderId", orderId, "storeId", order.storeId, "modelCode", order.modelCode, "paid", "PAID".equals(order.paymentStatus), "reason", reason));
        if ("PAID".equals(order.paymentStatus)) {
            outbox.append(EventTypes.REFUND_REQUESTED, orderId, Map.of("orderId", orderId, "amount", order.amount));
        }
        return require(orderId);
    }

    @Cacheable(cacheNames = "orders", key = "#orderId")
    public SalesOrderEntity get(String orderId, RequestUser user) {
        return requireAccessible(orderId, user);
    }

    public List<SalesOrderEntity> list(RequestUser user) {
        var query = new QueryWrapper<SalesOrderEntity>().orderByDesc("created_at");
        if (!user.isAdmin()) {
            query.eq("store_id", user.storeId());
        }
        return mapper.selectList(query);
    }

    private SalesOrderEntity requireAccessible(String orderId, RequestUser user) {
        var order = require(orderId);
        if (!user.isAdmin() && !user.storeId().equals(order.storeId)) {
            throw new BusinessException("ORDER_ACCESS_DENIED", "The order belongs to another store");
        }
        return order;
    }

    private SalesOrderEntity require(String orderId) {
        var order = mapper.selectById(orderId);
        if (order == null) throw new BusinessException("ORDER_NOT_FOUND", "Order not found");
        return order;
    }

    private void ensureUpdated(int rows) {
        if (rows != 1) throw new BusinessException("ORDER_CONCURRENTLY_CHANGED", "The order was changed by another request");
    }

    public record CreateOrderCommand(String channel, String channelOrderNo, String storeId, String customerName,
                                     String customerPhone, String modelCode, java.math.BigDecimal amount) {}
}
