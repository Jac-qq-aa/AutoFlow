package io.autoflow.order.api;

import io.autoflow.common.api.ApiResponse;
import io.autoflow.order.application.OrderService;
import io.autoflow.order.application.RequestUser;
import io.autoflow.order.persistence.SalesOrderEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    ApiResponse<SalesOrderEntity> create(@Valid @RequestBody CreateOrderRequest request,
                                         @RequestHeader("X-User-Id") String userId,
                                         @RequestHeader("X-User-Role") String role,
                                         @RequestHeader("X-Store-Id") String storeId) {
        var command = new OrderService.CreateOrderCommand(request.channel(), request.channelOrderNo(), request.storeId(),
            request.customerName(), request.customerPhone(), request.modelCode(), request.amount());
        return ApiResponse.ok(service.create(command, new RequestUser(userId, role, storeId)));
    }

    @GetMapping
    ApiResponse<List<SalesOrderEntity>> list(@RequestHeader("X-User-Id") String userId,
                                              @RequestHeader("X-User-Role") String role,
                                              @RequestHeader("X-Store-Id") String storeId) {
        return ApiResponse.ok(service.list(new RequestUser(userId, role, storeId)));
    }

    @GetMapping("/{orderId}")
    ApiResponse<SalesOrderEntity> get(@PathVariable String orderId,
                                      @RequestHeader("X-User-Id") String userId,
                                      @RequestHeader("X-User-Role") String role,
                                      @RequestHeader("X-Store-Id") String storeId) {
        return ApiResponse.ok(service.get(orderId, new RequestUser(userId, role, storeId)));
    }

    @PostMapping("/{orderId}/approve")
    ApiResponse<SalesOrderEntity> approve(@PathVariable String orderId,
                                          @RequestHeader("X-User-Id") String userId,
                                          @RequestHeader("X-User-Role") String role,
                                          @RequestHeader("X-Store-Id") String storeId) {
        return ApiResponse.ok(service.approve(orderId, new RequestUser(userId, role, storeId)));
    }

    @PostMapping("/{orderId}/pay")
    ApiResponse<SalesOrderEntity> pay(@PathVariable String orderId, @RequestBody(required = false) PaymentRequest request,
                                      @RequestHeader("X-User-Id") String userId,
                                      @RequestHeader("X-User-Role") String role,
                                      @RequestHeader("X-Store-Id") String storeId) {
        return ApiResponse.ok(service.requestPayment(orderId, new RequestUser(userId, role, storeId), request == null ? "SUCCESS" : request.scenario()));
    }

    @PostMapping("/{orderId}/cancel")
    ApiResponse<SalesOrderEntity> cancel(@PathVariable String orderId, @Valid @RequestBody CancelRequest request,
                                         @RequestHeader("X-User-Id") String userId,
                                         @RequestHeader("X-User-Role") String role,
                                         @RequestHeader("X-Store-Id") String storeId) {
        return ApiResponse.ok(service.cancel(orderId, new RequestUser(userId, role, storeId), request.reason()));
    }

    record CreateOrderRequest(@NotBlank String channel, String channelOrderNo, @NotBlank String storeId,
                              @NotBlank String customerName, @NotBlank String customerPhone,
                              @NotBlank String modelCode, @NotNull @DecimalMin("0.01") BigDecimal amount) {}
    record PaymentRequest(String scenario) {}
    record CancelRequest(@NotBlank String reason) {}
}
