package io.autoflow.order.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrderEntity> {
    @Update("""
        UPDATE sales_order
        SET status = #{nextStatus}, updated_at = CURRENT_TIMESTAMP, version = version + 1
        WHERE order_id = #{orderId} AND status = #{expectedStatus}
        """)
    int transition(@Param("orderId") String orderId, @Param("expectedStatus") String expectedStatus, @Param("nextStatus") String nextStatus);

    @Update("""
        UPDATE sales_order
        SET status = #{status}, inventory_status = #{inventoryStatus}, updated_at = CURRENT_TIMESTAMP, version = version + 1
        WHERE order_id = #{orderId}
        """)
    int updateInventoryState(@Param("orderId") String orderId, @Param("status") String status, @Param("inventoryStatus") String inventoryStatus);

    @Update("""
        UPDATE sales_order
        SET status = #{status}, payment_status = #{paymentStatus}, updated_at = CURRENT_TIMESTAMP, version = version + 1
        WHERE order_id = #{orderId}
        """)
    int updatePaymentState(@Param("orderId") String orderId, @Param("status") String status, @Param("paymentStatus") String paymentStatus);

    @Update("""
        UPDATE sales_order
        SET inventory_status = 'VIN_ALLOCATED', vin = #{vin},
            fulfillment_status = CASE WHEN fulfillment_status = 'NOT_STARTED' THEN 'READY' ELSE fulfillment_status END,
            updated_at = CURRENT_TIMESTAMP, version = version + 1
        WHERE order_id = #{orderId}
          AND status NOT IN ('CANCELLING','REFUNDING','CANCELLED','CLOSED','COMPLETED')
          AND (vin IS NULL OR vin = #{vin})
        """)
    int recordVinAllocated(@Param("orderId") String orderId, @Param("vin") String vin);

    @Update("UPDATE sales_order SET payment_status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status = 'PENDING_PAYMENT' AND payment_status IN ('UNPAID','FAILED')")
    int markPaymentProcessing(String orderId);

    @Update("UPDATE sales_order SET status = 'PENDING_PAYMENT', inventory_status = 'RESERVED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status = 'PENDING_STOCK'")
    int inventoryReserved(String orderId);

    @Update("UPDATE sales_order SET status = 'CLOSED', inventory_status = 'REJECTED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status = 'PENDING_STOCK'")
    int inventoryRejected(String orderId);

    @Update("""
        UPDATE sales_order
        SET payment_status = 'PAID',
            status = CASE WHEN status = 'CANCELLING' THEN 'REFUNDING' ELSE status END,
            updated_at = CURRENT_TIMESTAMP, version = version + 1
        WHERE order_id = #{orderId}
          AND status IN ('PENDING_PAYMENT','CANCELLING')
          AND payment_status <> 'PAID'
        """)
    int recordPaymentSucceeded(String orderId);

    @Update("UPDATE sales_order SET payment_status = 'FAILED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status IN ('PENDING_PAYMENT','CANCELLING') AND payment_status = 'PROCESSING'")
    int recordPaymentFailed(String orderId);

    @Update("UPDATE sales_order SET inventory_status = 'RELEASED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status IN ('CANCELLING','REFUNDING')")
    int inventoryReleased(String orderId);

    @Update("UPDATE sales_order SET payment_status = 'REFUNDED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status IN ('CANCELLING','REFUNDING')")
    int refundSucceeded(String orderId);

    @Update("UPDATE sales_order SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status IN ('CANCELLING','REFUNDING') AND inventory_status IN ('NOT_RESERVED','RELEASED') AND payment_status IN ('UNPAID','FAILED','REFUNDED')")
    int finalizeCancellation(String orderId);

    @Update("UPDATE sales_order SET fulfillment_status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status NOT IN ('CANCELLING','REFUNDING','CANCELLED','CLOSED')")
    int recordDeliveryCompleted(String orderId);

    @Update("""
        UPDATE sales_order
        SET status = CASE
                WHEN payment_status = 'PAID' AND vin IS NOT NULL AND fulfillment_status = 'COMPLETED' THEN 'COMPLETED'
                WHEN payment_status = 'PAID' AND vin IS NOT NULL THEN 'PENDING_DELIVERY'
                WHEN payment_status = 'PAID' THEN 'PENDING_VIN'
                ELSE status
            END,
            updated_at = CURRENT_TIMESTAMP,
            version = version + 1
        WHERE order_id = #{orderId}
          AND status NOT IN ('CANCELLING','REFUNDING','CANCELLED','CLOSED','COMPLETED')
        """)
    int reconcileFulfillmentState(String orderId);
}
