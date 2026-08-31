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
        SET status = 'PENDING_DELIVERY', inventory_status = 'VIN_ALLOCATED', vin = #{vin},
            fulfillment_status = 'READY', updated_at = CURRENT_TIMESTAMP, version = version + 1
        WHERE order_id = #{orderId} AND status = 'PENDING_VIN'
        """)
    int allocateVin(@Param("orderId") String orderId, @Param("vin") String vin);

    @Update("UPDATE sales_order SET payment_status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status = 'PENDING_PAYMENT' AND payment_status IN ('UNPAID','FAILED')")
    int markPaymentProcessing(String orderId);

    @Update("UPDATE sales_order SET status = 'PENDING_PAYMENT', inventory_status = 'RESERVED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status = 'PENDING_STOCK'")
    int inventoryReserved(String orderId);

    @Update("UPDATE sales_order SET status = 'CLOSED', inventory_status = 'REJECTED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status = 'PENDING_STOCK'")
    int inventoryRejected(String orderId);

    @Update("UPDATE sales_order SET status = 'PENDING_VIN', payment_status = 'PAID', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status = 'PENDING_PAYMENT'")
    int paymentSucceeded(String orderId);

    @Update("UPDATE sales_order SET payment_status = 'FAILED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status = 'PENDING_PAYMENT'")
    int paymentFailed(String orderId);

    @Update("UPDATE sales_order SET inventory_status = 'RELEASED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status = 'CANCELLING'")
    int inventoryReleased(String orderId);

    @Update("UPDATE sales_order SET payment_status = 'REFUNDED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status IN ('CANCELLING','REFUNDING')")
    int refundSucceeded(String orderId);

    @Update("UPDATE sales_order SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status IN ('CANCELLING','REFUNDING') AND inventory_status IN ('NOT_RESERVED','RELEASED') AND payment_status IN ('UNPAID','FAILED','REFUNDED')")
    int finalizeCancellation(String orderId);

    @Update("UPDATE sales_order SET status = 'COMPLETED', fulfillment_status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE order_id = #{orderId} AND status = 'PENDING_DELIVERY'")
    int deliveryCompleted(String orderId);
}
