package io.autoflow.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.autoflow.common.error.AccessDeniedException;
import io.autoflow.messaging.DomainEvent;
import io.autoflow.messaging.EventTypes;
import io.autoflow.order.application.OrderEventHandler;
import io.autoflow.order.application.OrderService;
import io.autoflow.order.application.OrderRecordService;
import io.autoflow.order.application.RequestUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
    "autoflow.messaging.enabled=false",
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
class OrderConsistencyIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("autoflow_order").withUsername("autoflow").withPassword("autoflow123");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.2.9-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    private static final RequestUser ADMIN = new RequestUser("admin", "ADMIN", "*");
    private static final RequestUser SH_SALES = new RequestUser("sales", "SALES", "STORE-SH-001");

    @Autowired OrderService orders;
    @Autowired OrderRecordService records;
    @Autowired OrderEventHandler events;
    @Autowired ObjectMapper objectMapper;

    @Test
    void vinBeforePaymentStillConvergesToPendingDelivery() {
        var orderId = pendingPaymentOrder("STORE-SH-001");

        events.handle(event(EventTypes.VIN_ALLOCATED, orderId, Map.of("vin", "LTEST000000000001")));
        events.handle(event(EventTypes.PAYMENT_SUCCEEDED, orderId, Map.of("paymentId", UUID.randomUUID().toString())));

        var order = orders.get(orderId, ADMIN);
        assertThat(order.status).isEqualTo("PENDING_DELIVERY");
        assertThat(order.paymentStatus).isEqualTo("PAID");
        assertThat(order.vin).isEqualTo("LTEST000000000001");
    }

    @Test
    void paymentSuccessAfterCancellationMovesOrderToRefunding() {
        var orderId = pendingPaymentOrder("STORE-SH-001");
        orders.cancel(orderId, ADMIN, "customer request during payment");

        events.handle(event(EventTypes.PAYMENT_SUCCEEDED, orderId, Map.of("paymentId", UUID.randomUUID().toString())));

        var order = orders.get(orderId, ADMIN);
        assertThat(order.status).isEqualTo("REFUNDING");
        assertThat(order.paymentStatus).isEqualTo("PAID");
    }

    @Test
    void lateInventoryReleaseAfterPaymentSuccessStillCompletesCancellation() {
        var orderId = pendingPaymentOrder("STORE-SH-001");
        orders.cancel(orderId, ADMIN, "customer request during payment");
        events.handle(event(EventTypes.PAYMENT_SUCCEEDED, orderId, Map.of("paymentId", UUID.randomUUID().toString())));

        events.handle(event(EventTypes.INVENTORY_RELEASED, orderId, Map.of("reservationId", UUID.randomUUID().toString())));
        events.handle(event(EventTypes.REFUND_SUCCEEDED, orderId, Map.of("refundId", UUID.randomUUID().toString())));

        var order = orders.get(orderId, ADMIN);
        assertThat(order.status).isEqualTo("CANCELLED");
        assertThat(order.inventoryStatus).isEqualTo("RELEASED");
        assertThat(order.paymentStatus).isEqualTo("REFUNDED");
    }

    @Test
    void cachedOrderStillEnforcesStoreBoundary() {
        var order = orders.create(command("STORE-BJ-001"), ADMIN);
        assertThat(orders.get(order.orderId, ADMIN).orderId).isEqualTo(order.orderId);

        assertThatThrownBy(() -> orders.get(order.orderId, SH_SALES))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("another store");
    }

    @Test
    void subpageRecordsAreExtensibleAndStoreScoped() {
        var order = orders.create(command("STORE-BJ-001"), ADMIN);

        var created = records.create(order.orderId, "INSURANCE", "insurance-page",
            Map.of("provider", "AutoFlow Insurance", "policyNo", "POL-10001"), ADMIN);

        assertThat(created.recordType).isEqualTo("INSURANCE");
        assertThat(records.list(order.orderId, ADMIN)).extracting(item -> item.sourcePage).containsExactly("insurance-page");
        assertThatThrownBy(() -> records.list(order.orderId, SH_SALES)).hasMessageContaining("another store");
    }

    @Test
    void duplicateDomainEventIsIdempotent() {
        var orderId = pendingPaymentOrder("STORE-SH-001");
        var paymentSucceeded = event(EventTypes.PAYMENT_SUCCEEDED, orderId, Map.of("paymentId", UUID.randomUUID().toString()));

        events.handle(paymentSucceeded);
        events.handle(paymentSucceeded);

        assertThat(orders.get(orderId, ADMIN).paymentStatus).isEqualTo("PAID");
    }

    private String pendingPaymentOrder(String storeId) {
        var order = orders.create(command(storeId), ADMIN);
        orders.approve(order.orderId, ADMIN);
        events.handle(event(EventTypes.INVENTORY_RESERVED, order.orderId, Map.of("reservationId", UUID.randomUUID().toString())));
        orders.requestPayment(order.orderId, ADMIN, "SUCCESS");
        return order.orderId;
    }

    private OrderService.CreateOrderCommand command(String storeId) {
        return new OrderService.CreateOrderCommand("STORE", "TEST-" + UUID.randomUUID(), storeId,
            "测试客户", "13800000000", "AF-SUV-PRO", new BigDecimal("219800"));
    }

    private DomainEvent event(String type, String orderId, Map<String, Object> payload) {
        return new DomainEvent(UUID.randomUUID(), type, orderId, Instant.now(), objectMapper.valueToTree(payload));
    }
}
