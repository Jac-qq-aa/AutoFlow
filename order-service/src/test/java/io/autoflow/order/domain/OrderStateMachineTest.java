package io.autoflow.order.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.autoflow.common.error.BusinessException;
import org.junit.jupiter.api.Test;

class OrderStateMachineTest {
    private final OrderStateMachine machine = new OrderStateMachine();

    @Test
    void onlyPendingReviewCanBeApproved() {
        assertThatCode(() -> machine.requireApprovalAllowed(OrderStatus.PENDING_REVIEW)).doesNotThrowAnyException();
        assertThatThrownBy(() -> machine.requireApprovalAllowed(OrderStatus.PENDING_PAYMENT)).isInstanceOf(BusinessException.class);
    }

    @Test
    void completedOrderCannotBeCancelled() {
        assertThatThrownBy(() -> machine.requireCancellationAllowed(OrderStatus.COMPLETED)).isInstanceOf(BusinessException.class);
        assertThatCode(() -> machine.requireCancellationAllowed(OrderStatus.PENDING_DELIVERY)).doesNotThrowAnyException();
    }
}

