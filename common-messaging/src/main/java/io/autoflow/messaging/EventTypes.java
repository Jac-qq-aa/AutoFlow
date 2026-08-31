package io.autoflow.messaging;

public final class EventTypes {
    public static final String INVENTORY_RESERVATION_REQUESTED = "InventoryReservationRequested";
    public static final String INVENTORY_RESERVED = "InventoryReserved";
    public static final String INVENTORY_REJECTED = "InventoryRejected";
    public static final String PAYMENT_REQUESTED = "PaymentRequested";
    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String VIN_ALLOCATED = "VinAllocated";
    public static final String ORDER_CANCELLATION_REQUESTED = "OrderCancellationRequested";
    public static final String INVENTORY_RELEASED = "InventoryReleased";
    public static final String REFUND_REQUESTED = "RefundRequested";
    public static final String REFUND_SUCCEEDED = "RefundSucceeded";
    public static final String DELIVERY_COMPLETED = "DeliveryCompleted";

    private EventTypes() {}
}

