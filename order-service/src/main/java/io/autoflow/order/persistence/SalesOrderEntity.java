package io.autoflow.order.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("sales_order")
public class SalesOrderEntity {
    @TableId public String orderId;
    public String orderNo;
    public String channel;
    public String channelOrderNo;
    public String storeId;
    public String customerName;
    public String customerPhone;
    public String modelCode;
    public BigDecimal amount;
    public String status;
    public String inventoryStatus;
    public String paymentStatus;
    public String fulfillmentStatus;
    public String vin;
    @Version public Integer version;
    public String createdBy;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}

