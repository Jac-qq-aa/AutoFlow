package io.autoflow.order.api;

import io.autoflow.common.api.ApiResponse;
import io.autoflow.order.application.OrderRecordService;
import io.autoflow.order.application.RequestUser;
import io.autoflow.order.persistence.OrderRecordEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/{orderId}/records")
public class OrderRecordController {
    private final OrderRecordService service;

    public OrderRecordController(OrderRecordService service) { this.service = service; }

    @GetMapping
    ApiResponse<List<OrderRecordEntity>> list(@PathVariable String orderId,
                                              @RequestHeader("X-User-Id") String userId,
                                              @RequestHeader("X-User-Role") String role,
                                              @RequestHeader("X-Store-Id") String storeId) {
        return ApiResponse.ok(service.list(orderId, new RequestUser(userId, role, storeId)));
    }

    @PostMapping
    ApiResponse<OrderRecordEntity> create(@PathVariable String orderId, @Valid @RequestBody CreateRecordRequest request,
                                          @RequestHeader("X-User-Id") String userId,
                                          @RequestHeader("X-User-Role") String role,
                                          @RequestHeader("X-Store-Id") String storeId) {
        return ApiResponse.ok(service.create(orderId, request.recordType(), request.sourcePage(), request.fields(),
            new RequestUser(userId, role, storeId)));
    }

    record CreateRecordRequest(@NotBlank String recordType, @NotBlank String sourcePage,
                               @NotEmpty Map<String, Object> fields) {}
}
