package io.autoflow.inventory.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventEntity> {
    @Select("SELECT * FROM outbox_event WHERE status = 'PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP ORDER BY created_at LIMIT 50")
    List<OutboxEventEntity> findPublishable();
    @Update("UPDATE outbox_event SET status = 'SENT', sent_at = CURRENT_TIMESTAMP WHERE event_id = #{eventId} AND status = 'PENDING'") int markSent(String eventId);
    @Update("UPDATE outbox_event SET attempts = attempts + 1, next_attempt_at = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL LEAST(60, POW(2, attempts)) SECOND) WHERE event_id = #{eventId} AND status = 'PENDING'") int scheduleRetry(String eventId);
}

