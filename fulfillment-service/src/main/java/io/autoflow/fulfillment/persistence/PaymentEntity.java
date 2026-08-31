package io.autoflow.fulfillment.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("payment")
public class PaymentEntity {
    @TableId public String paymentId;
    public String orderId;
    public BigDecimal amount;
    public String scenario;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}

