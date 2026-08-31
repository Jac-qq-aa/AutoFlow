package io.autoflow.inventory.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("vehicle")
public class VehicleEntity {
    @TableId public String vin;
    public String storeId;
    public String modelCode;
    public String color;
    public String status;
    public String allocatedOrderId;
    public LocalDateTime updatedAt;
}

