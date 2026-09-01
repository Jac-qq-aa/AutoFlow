package io.autoflow.fulfillment.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.autoflow.common.web.RequestActor;
import io.autoflow.fulfillment.persistence.DeliveryTaskEntity;
import io.autoflow.fulfillment.persistence.DeliveryTaskMapper;
import io.autoflow.fulfillment.persistence.PaymentMapper;
import io.autoflow.fulfillment.persistence.RefundMapper;
import org.junit.jupiter.api.Test;

class FulfillmentAuthorizationTest {
    private final PaymentMapper payments = mock(PaymentMapper.class);
    private final RefundMapper refunds = mock(RefundMapper.class);
    private final DeliveryTaskMapper deliveries = mock(DeliveryTaskMapper.class);
    private final FulfillmentOutbox outbox = mock(FulfillmentOutbox.class);
    private final FulfillmentService service = new FulfillmentService(payments, refunds, deliveries, outbox);

    @Test
    void deliveryUserCannotCompleteAnotherStoresTask() {
        var task = new DeliveryTaskEntity();
        task.orderId = "order-1";
        task.storeId = "STORE-BJ-001";
        when(deliveries.findByOrderId("order-1")).thenReturn(task);

        assertThatThrownBy(() -> service.completeDelivery("order-1", new RequestActor("courier", "DELIVERY", "STORE-SH-001")))
            .hasMessageContaining("own store");

        verifyNoInteractions(outbox);
    }

    @Test
    void salesCannotCompleteDeliveryEvenForOwnStore() {
        assertThatThrownBy(() -> service.completeDelivery("order-1", new RequestActor("sales", "SALES", "STORE-SH-001")))
            .hasMessageContaining("DELIVERY");
    }
}
