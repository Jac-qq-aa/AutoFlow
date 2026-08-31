package io.autoflow.fulfillment.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("refund")
public class RefundEntity {
    @TableId public String refundId;
    public String orderId;
    public BigDecimal amount;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}

