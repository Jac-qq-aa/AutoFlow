package io.autoflow.order.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sales_order_record")
public class OrderRecordEntity {
    @TableId public String recordId;
    public String orderId;
    public String storeId;
    public String recordType;
    public String sourcePage;
    public String recordData;
    public String createdBy;
    public LocalDateTime createdAt;
}
