package io.autoflow.inventory.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("inventory_reservation")
public class ReservationEntity {
    @TableId public String reservationId;
    public String orderId;
    public String storeId;
    public String modelCode;
    public String vin;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}

