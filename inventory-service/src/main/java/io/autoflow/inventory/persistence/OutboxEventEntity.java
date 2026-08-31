package io.autoflow.inventory.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("outbox_event")
public class OutboxEventEntity {
    @TableId public String eventId;
    public String aggregateId;
    public String eventType;
    public String payload;
    public String status;
    public Integer attempts;
    public LocalDateTime nextAttemptAt;
    public LocalDateTime createdAt;
    public LocalDateTime sentAt;
}

