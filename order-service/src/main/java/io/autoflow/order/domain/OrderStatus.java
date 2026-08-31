package io.autoflow.order.domain;

public enum OrderStatus {
    PENDING_REVIEW,
    PENDING_STOCK,
    PENDING_PAYMENT,
    PENDING_VIN,
    PENDING_DELIVERY,
    COMPLETED,
    CANCELLING,
    REFUNDING,
    REFUNDED,
    CANCELLED,
    CLOSED
}

