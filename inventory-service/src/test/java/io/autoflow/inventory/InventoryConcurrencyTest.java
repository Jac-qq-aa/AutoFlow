package io.autoflow.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.autoflow.inventory.application.InventoryService;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
    "autoflow.messaging.enabled=false",
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
class InventoryConcurrencyTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("autoflow_inventory").withUsername("autoflow").withPassword("autoflow123");

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

    @Autowired InventoryService service;

    @Test
    void concurrentReservationsNeverMakeQuotaNegative() throws Exception {
        int initial = service.quota("STORE-SH-001", "AF-SUV-PRO").available;
        var executor = Executors.newFixedThreadPool(12);
        try {
            var futures = IntStream.range(0, 20)
                .mapToObj(index -> executor.submit(() -> {
                    try {
                        service.reserve(UUID.randomUUID().toString(), "STORE-SH-001", "AF-SUV-PRO");
                        return true;
                    } catch (RuntimeException rejected) {
                        return false;
                    }
                })).toList();
            int successes = 0;
            for (var future : futures) if (future.get()) successes++;
            var after = service.quota("STORE-SH-001", "AF-SUV-PRO");
            assertThat(after.available).isZero();
            assertThat(successes).isEqualTo(initial);
            assertThat(after.reserved).isEqualTo(initial);
        } finally {
            executor.shutdownNow();
        }
    }
}
