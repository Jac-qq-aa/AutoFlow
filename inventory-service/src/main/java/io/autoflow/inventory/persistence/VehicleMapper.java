package io.autoflow.inventory.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VehicleMapper extends BaseMapper<VehicleEntity> {
    @Select("SELECT * FROM vehicle WHERE store_id = #{storeId} AND model_code = #{modelCode} AND status = 'AVAILABLE' ORDER BY vin LIMIT 1 FOR UPDATE")
    VehicleEntity lockOneAvailable(@Param("storeId") String storeId, @Param("modelCode") String modelCode);

    @Update("UPDATE vehicle SET status = 'ALLOCATED', allocated_order_id = #{orderId}, updated_at = CURRENT_TIMESTAMP WHERE vin = #{vin} AND status = 'AVAILABLE'")
    int allocate(@Param("vin") String vin, @Param("orderId") String orderId);

    @Update("UPDATE vehicle SET status = 'AVAILABLE', allocated_order_id = NULL, updated_at = CURRENT_TIMESTAMP WHERE allocated_order_id = #{orderId} AND status = 'ALLOCATED'")
    int releaseByOrderId(@Param("orderId") String orderId);
}

