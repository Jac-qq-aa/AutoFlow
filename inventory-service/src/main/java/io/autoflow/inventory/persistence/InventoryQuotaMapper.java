package io.autoflow.inventory.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryQuotaMapper extends BaseMapper<InventoryQuotaEntity> {
    @Select("SELECT * FROM inventory_quota WHERE store_id = #{storeId} AND model_code = #{modelCode}")
    InventoryQuotaEntity find(@Param("storeId") String storeId, @Param("modelCode") String modelCode);

    @Update("""
        UPDATE inventory_quota
        SET available = available - 1, reserved = reserved + 1, version = version + 1, updated_at = CURRENT_TIMESTAMP
        WHERE store_id = #{storeId} AND model_code = #{modelCode} AND available > 0
        """)
    int reserve(@Param("storeId") String storeId, @Param("modelCode") String modelCode);

    @Update("""
        UPDATE inventory_quota
        SET available = available + 1, reserved = reserved - 1, version = version + 1, updated_at = CURRENT_TIMESTAMP
        WHERE store_id = #{storeId} AND model_code = #{modelCode} AND reserved > 0
        """)
    int release(@Param("storeId") String storeId, @Param("modelCode") String modelCode);
}

