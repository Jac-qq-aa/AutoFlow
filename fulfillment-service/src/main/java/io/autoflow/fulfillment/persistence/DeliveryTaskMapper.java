package io.autoflow.fulfillment.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DeliveryTaskMapper extends BaseMapper<DeliveryTaskEntity> {
    @Select("SELECT * FROM delivery_task WHERE order_id = #{orderId}") DeliveryTaskEntity findByOrderId(String orderId);
    @Update("UPDATE delivery_task SET status = 'COMPLETED', completed_by = #{userId}, completed_at = CURRENT_TIMESTAMP WHERE order_id = #{orderId} AND status = 'PENDING'") int complete(String orderId, String userId);
}

