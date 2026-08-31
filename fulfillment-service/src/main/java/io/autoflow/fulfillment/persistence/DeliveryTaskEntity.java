package io.autoflow.fulfillment.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("delivery_task")
public class DeliveryTaskEntity {
    @TableId public String taskId;
    public String orderId;
    public String vin;
    public String status;
    public String completedBy;
    public LocalDateTime createdAt;
    public LocalDateTime completedAt;
}

