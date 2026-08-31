package io.autoflow.fulfillment.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RefundMapper extends BaseMapper<RefundEntity> {
    @Select("SELECT * FROM refund WHERE order_id = #{orderId}") RefundEntity findByOrderId(String orderId);
}

