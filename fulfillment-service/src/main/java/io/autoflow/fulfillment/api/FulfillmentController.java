package io.autoflow.fulfillment.api;

import io.autoflow.common.api.ApiResponse;
import io.autoflow.fulfillment.application.FulfillmentService;
import io.autoflow.fulfillment.persistence.DeliveryTaskEntity;
import io.autoflow.fulfillment.persistence.PaymentEntity;
import io.autoflow.fulfillment.persistence.RefundEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fulfillment")
public class FulfillmentController {
    private final FulfillmentService service;
    public FulfillmentController(FulfillmentService service) { this.service = service; }

    @PostMapping("/payments")
    ApiResponse<PaymentEntity> pay(@Valid @RequestBody PaymentRequest request) {
        return ApiResponse.ok(service.simulatePayment(request.orderId(), request.amount(), request.scenario()));
    }

    @PostMapping("/refunds")
    ApiResponse<RefundEntity> refund(@Valid @RequestBody RefundRequest request) {
        return ApiResponse.ok(service.refund(request.orderId(), request.amount()));
    }

    @PostMapping("/deliveries")
    ApiResponse<DeliveryTaskEntity> createDelivery(@Valid @RequestBody DeliveryRequest request) {
        return ApiResponse.ok(service.createDelivery(request.orderId(), request.vin()));
    }

    @PostMapping("/deliveries/{orderId}/complete")
    ApiResponse<DeliveryTaskEntity> complete(@PathVariable String orderId,
                                             @RequestHeader("X-User-Id") String userId,
                                             @RequestHeader("X-User-Role") String role) {
        return ApiResponse.ok(service.completeDelivery(orderId, userId, role));
    }

    record PaymentRequest(@NotBlank String orderId, @DecimalMin("0.01") BigDecimal amount, String scenario) {}
    record RefundRequest(@NotBlank String orderId, @DecimalMin("0.01") BigDecimal amount) {}
    record DeliveryRequest(@NotBlank String orderId, @NotBlank String vin) {}
}

