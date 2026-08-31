package io.autoflow.order.domain;

import io.autoflow.common.error.BusinessException;
import java.util.EnumSet;

public final class OrderStateMachine {
    private static final EnumSet<OrderStatus> CANCELLABLE = EnumSet.of(
        OrderStatus.PENDING_REVIEW,
        OrderStatus.PENDING_STOCK,
        OrderStatus.PENDING_PAYMENT,
        OrderStatus.PENDING_VIN,
        OrderStatus.PENDING_DELIVERY
    );

    public void requireApprovalAllowed(OrderStatus current) {
        require(current == OrderStatus.PENDING_REVIEW, "ORDER_NOT_REVIEWABLE", "Only a pending-review order can be approved");
    }

    public void requirePaymentAllowed(OrderStatus current) {
        require(current == OrderStatus.PENDING_PAYMENT, "ORDER_NOT_PAYABLE", "Inventory must be reserved before payment");
    }

    public void requireCancellationAllowed(OrderStatus current) {
        require(CANCELLABLE.contains(current), "ORDER_NOT_CANCELLABLE", "The order cannot be cancelled in status " + current);
    }

    private void require(boolean condition, String code, String message) {
        if (!condition) {
            throw new BusinessException(code, message);
        }
    }
}

