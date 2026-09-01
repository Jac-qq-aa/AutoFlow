package io.autoflow.order.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.autoflow.common.error.BusinessException;
import io.autoflow.order.persistence.OrderRecordEntity;
import io.autoflow.order.persistence.OrderRecordMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderRecordService {
    private static final Pattern RECORD_TYPE = Pattern.compile("[A-Z][A-Z0-9_]{1,31}");
    private final OrderRecordMapper records;
    private final OrderService orders;
    private final ObjectMapper objectMapper;

    public OrderRecordService(OrderRecordMapper records, OrderService orders, ObjectMapper objectMapper) {
        this.records = records;
        this.orders = orders;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderRecordEntity create(String orderId, String recordType, String sourcePage,
                                    Map<String, Object> fields, RequestUser user) {
        user.requireAnyRole("SALES", "STORE_MANAGER", "ADMIN");
        var order = orders.get(orderId, user);
        var normalizedType = recordType == null ? "" : recordType.trim().toUpperCase();
        if (!RECORD_TYPE.matcher(normalizedType).matches()) {
            throw new BusinessException("INVALID_RECORD_TYPE", "Record type must be an uppercase business code");
        }
        if (fields == null || fields.isEmpty()) throw new BusinessException("EMPTY_RECORD", "Record fields cannot be empty");
        try {
            var record = new OrderRecordEntity();
            record.recordId = UUID.randomUUID().toString();
            record.orderId = orderId;
            record.storeId = order.storeId;
            record.recordType = normalizedType;
            record.sourcePage = sourcePage;
            record.recordData = objectMapper.writeValueAsString(fields);
            record.createdBy = user.userId();
            record.createdAt = LocalDateTime.now();
            records.insert(record);
            return record;
        } catch (Exception exception) {
            throw new BusinessException("RECORD_SERIALIZATION_FAILED", "Order record could not be stored");
        }
    }

    public List<OrderRecordEntity> list(String orderId, RequestUser user) {
        orders.get(orderId, user);
        return records.findByOrderId(orderId);
    }
}
