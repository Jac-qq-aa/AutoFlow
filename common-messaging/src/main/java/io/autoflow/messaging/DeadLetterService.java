package io.autoflow.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class DeadLetterService implements DeadLetterRecorder {
    private final JdbcTemplate jdbc; private final ObjectMapper objectMapper; private final EventPublisher publisher;
    public DeadLetterService(JdbcTemplate jdbc, ObjectMapper objectMapper, EventPublisher publisher) {
        this.jdbc = jdbc; this.objectMapper = objectMapper; this.publisher = publisher;
    }

    @Override
    @Transactional
    public void record(String topic, DomainEvent event, String reason) {
        try {
            jdbc.update("""
                INSERT INTO dead_letter_event(id,topic,event_id,event_type,aggregate_id,event_json,reason,status,created_at)
                VALUES (?,?,?,?,?,?,?,'PENDING',?)
                """, UUID.randomUUID().toString(), topic, event.eventId().toString(), event.eventType(), event.aggregateId(),
                objectMapper.writeValueAsString(event), abbreviate(reason, 2000), Timestamp.from(Instant.now()));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not persist dead letter", exception);
        }
    }

    public List<Map<String, Object>> list() {
        return jdbc.queryForList("SELECT id,topic,event_id,event_type,aggregate_id,reason,status,created_at,replayed_at FROM dead_letter_event ORDER BY created_at DESC LIMIT 200");
    }

    @Transactional
    public void replay(String id) {
        try {
            var row = jdbc.queryForMap("SELECT topic,event_json,status FROM dead_letter_event WHERE id = ? FOR UPDATE", id);
            if ("REPLAYED".equals(row.get("status"))) return;
            var event = objectMapper.readValue(String.valueOf(row.get("event_json")), DomainEvent.class);
            publisher.publish(String.valueOf(row.get("topic")), event);
            jdbc.update("UPDATE dead_letter_event SET status='REPLAYED', replayed_at=CURRENT_TIMESTAMP WHERE id=?", id);
        } catch (Exception exception) {
            throw new IllegalStateException("Dead letter replay failed", exception);
        }
    }

    private String abbreviate(String value, int max) {
        if (value == null) return "Unknown consumer failure";
        return value.length() <= max ? value : value.substring(0, max);
    }
}

