package io.autoflow.order.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderRecordMapper extends BaseMapper<OrderRecordEntity> {
    @Select("SELECT * FROM sales_order_record WHERE order_id = #{orderId} ORDER BY created_at, record_id")
    List<OrderRecordEntity> findByOrderId(String orderId);
}
