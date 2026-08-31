package io.autoflow.inventory.api;

import io.autoflow.common.api.ApiResponse;
import io.autoflow.inventory.application.InventoryService;
import io.autoflow.inventory.persistence.InventoryQuotaEntity;
import io.autoflow.inventory.persistence.ReservationEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) { this.service = service; }

    @GetMapping("/quota")
    ApiResponse<InventoryQuotaEntity> quota(@RequestParam String storeId, @RequestParam String modelCode) {
        return ApiResponse.ok(service.quota(storeId, modelCode));
    }

    @PostMapping("/reservations")
    ApiResponse<ReservationEntity> reserve(@Valid @RequestBody ReserveRequest request) {
        return ApiResponse.ok(service.reserve(request.orderId(), request.storeId(), request.modelCode()));
    }

    @PostMapping("/reservations/{orderId}/allocate-vin")
    ApiResponse<String> allocateVin(@PathVariable String orderId) {
        return ApiResponse.ok(service.allocateVin(orderId));
    }

    @PostMapping("/reservations/{orderId}/release")
    ApiResponse<Void> release(@PathVariable String orderId) {
        service.release(orderId);
        return ApiResponse.ok(null);
    }

    record ReserveRequest(@NotBlank String orderId, @NotBlank String storeId, @NotBlank String modelCode) {}
}

