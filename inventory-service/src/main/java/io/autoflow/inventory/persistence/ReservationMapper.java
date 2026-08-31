package io.autoflow.inventory.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReservationMapper extends BaseMapper<ReservationEntity> {
    @Select("SELECT * FROM inventory_reservation WHERE order_id = #{orderId}")
    ReservationEntity findByOrderId(String orderId);

    @Update("UPDATE inventory_reservation SET vin = #{vin}, status = 'VIN_ALLOCATED', updated_at = CURRENT_TIMESTAMP WHERE order_id = #{orderId} AND status = 'RESERVED'")
    int markVinAllocated(String orderId, String vin);

    @Update("UPDATE inventory_reservation SET status = 'RELEASED', updated_at = CURRENT_TIMESTAMP WHERE order_id = #{orderId} AND status IN ('RESERVED', 'VIN_ALLOCATED')")
    int markReleased(String orderId);
}

