package io.autoflow.fulfillment.application;

import io.autoflow.common.error.BusinessException;
import io.autoflow.common.web.RequestActor;
import io.autoflow.fulfillment.persistence.DeliveryTaskEntity;
import io.autoflow.fulfillment.persistence.DeliveryTaskMapper;
import io.autoflow.fulfillment.persistence.PaymentEntity;
import io.autoflow.fulfillment.persistence.PaymentMapper;
import io.autoflow.fulfillment.persistence.RefundEntity;
import io.autoflow.fulfillment.persistence.RefundMapper;
import io.autoflow.messaging.EventTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FulfillmentService {
    private final PaymentMapper paymentMapper; private final RefundMapper refundMapper;
    private final DeliveryTaskMapper deliveryMapper; private final FulfillmentOutbox outbox;
    public FulfillmentService(PaymentMapper paymentMapper, RefundMapper refundMapper, DeliveryTaskMapper deliveryMapper, FulfillmentOutbox outbox) {
        this.paymentMapper = paymentMapper; this.refundMapper = refundMapper; this.deliveryMapper = deliveryMapper; this.outbox = outbox;
    }

    @Transactional
    public PaymentEntity simulatePayment(String orderId, BigDecimal amount, String scenario) {
        var existing = paymentMapper.findByOrderId(orderId);
        if (existing != null) return existing;
        var normalized = scenario == null ? "SUCCESS" : scenario.toUpperCase();
        if (!java.util.Set.of("SUCCESS", "FAILURE", "TIMEOUT").contains(normalized)) {
            throw new BusinessException("UNKNOWN_PAYMENT_SCENARIO", "Use SUCCESS, FAILURE or TIMEOUT");
        }
        var payment = new PaymentEntity(); payment.paymentId = UUID.randomUUID().toString(); payment.orderId = orderId;
        payment.amount = amount; payment.scenario = normalized; payment.status = "PROCESSING";
        payment.createdAt = LocalDateTime.now(); payment.updatedAt = payment.createdAt; paymentMapper.insert(payment);
        if ("SUCCESS".equals(normalized)) finishPayment(payment, "SUCCEEDED", EventTypes.PAYMENT_SUCCEEDED, null);
        if ("FAILURE".equals(normalized)) finishPayment(payment, "FAILED", EventTypes.PAYMENT_FAILED, "SIMULATED_DECLINE");
        return paymentMapper.findByOrderId(orderId);
    }

    private void finishPayment(PaymentEntity payment, String status, String eventType, String reason) {
        if (paymentMapper.finish(payment.paymentId, status) == 1) {
            var payload = reason == null ? Map.of("orderId", payment.orderId, "paymentId", payment.paymentId)
                : Map.of("orderId", payment.orderId, "paymentId", payment.paymentId, "reason", reason);
            outbox.append(eventType, payment.orderId, payload);
        }
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void expireTimedOutPayments() {
        for (var payment : paymentMapper.findTimedOut()) finishPayment(payment, "FAILED", EventTypes.PAYMENT_FAILED, "PAYMENT_TIMEOUT");
    }

    @Transactional
    public RefundEntity refund(String orderId, BigDecimal amount) {
        var existing = refundMapper.findByOrderId(orderId);
        if (existing != null) return existing;
        var refund = new RefundEntity(); refund.refundId = UUID.randomUUID().toString(); refund.orderId = orderId;
        refund.amount = amount; refund.status = "SUCCEEDED"; refund.createdAt = LocalDateTime.now(); refund.updatedAt = refund.createdAt;
        refundMapper.insert(refund);
        outbox.append(EventTypes.REFUND_SUCCEEDED, orderId, Map.of("orderId", orderId, "refundId", refund.refundId));
        return refund;
    }

    @Transactional
    public DeliveryTaskEntity createDelivery(String orderId, String storeId, String vin) {
        var existing = deliveryMapper.findByOrderId(orderId);
        if (existing != null) return existing;
        var task = new DeliveryTaskEntity(); task.taskId = UUID.randomUUID().toString(); task.orderId = orderId;
        task.storeId = storeId; task.vin = vin; task.status = "PENDING"; task.createdAt = LocalDateTime.now(); deliveryMapper.insert(task); return task;
    }

    @Transactional
    public DeliveryTaskEntity completeDelivery(String orderId, RequestActor actor) {
        actor.requireAnyRole("DELIVERY", "ADMIN");
        var task = deliveryMapper.findByOrderId(orderId);
        if (task == null) throw new BusinessException("DELIVERY_NOT_FOUND", "Delivery task not found");
        actor.requireStore(task.storeId);
        if (deliveryMapper.complete(orderId, actor.userId()) != 1) throw new BusinessException("DELIVERY_NOT_PENDING", "Pending delivery task not found");
        outbox.append(EventTypes.DELIVERY_COMPLETED, orderId, Map.of("orderId", orderId, "completedBy", actor.userId()));
        return deliveryMapper.findByOrderId(orderId);
    }
}
