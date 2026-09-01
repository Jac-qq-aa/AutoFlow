package io.autoflow.messaging;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;

public class BusinessMetrics {
    public BusinessMetrics(JdbcTemplate jdbc, MeterRegistry registry) {
        Gauge.builder("autoflow.outbox.pending", jdbc, source -> count(source,
                "SELECT COUNT(*) FROM outbox_event WHERE status = 'PENDING'"))
            .description("Transactional outbox events waiting for publication")
            .register(registry);
        Gauge.builder("autoflow.dead.letters.pending", jdbc, source -> count(source,
                "SELECT COUNT(*) FROM dead_letter_event WHERE status = 'PENDING'"))
            .description("Consumer failures waiting for operational replay")
            .register(registry);
    }

    private static double count(JdbcTemplate jdbc, String sql) {
        try {
            var value = jdbc.queryForObject(sql, Long.class);
            return value == null ? 0 : value.doubleValue();
        } catch (RuntimeException databaseNotReady) {
            return Double.NaN;
        }
    }
}
