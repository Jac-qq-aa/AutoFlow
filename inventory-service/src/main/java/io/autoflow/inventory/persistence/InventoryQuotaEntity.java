package io.autoflow.inventory.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;

@TableName("inventory_quota")
public class InventoryQuotaEntity {
    @TableId public Long id;
    public String storeId;
    public String modelCode;
    public Integer available;
    public Integer reserved;
    @Version public Integer version;
    public LocalDateTime updatedAt;
}

