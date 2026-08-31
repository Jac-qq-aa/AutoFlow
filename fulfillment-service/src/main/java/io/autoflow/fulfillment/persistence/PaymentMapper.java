package io.autoflow.fulfillment.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentMapper extends BaseMapper<PaymentEntity> {
    @Select("SELECT * FROM payment WHERE order_id = #{orderId}") PaymentEntity findByOrderId(String orderId);
    @Select("SELECT * FROM payment WHERE status = 'PROCESSING' AND scenario = 'TIMEOUT' AND updated_at < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 15 SECOND)") List<PaymentEntity> findTimedOut();
    @Update("UPDATE payment SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE payment_id = #{paymentId} AND status = 'PROCESSING'") int finish(String paymentId, String status);
}

