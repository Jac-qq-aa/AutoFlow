package io.autoflow.inventory.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.autoflow.inventory.application.InventoryService;
import org.junit.jupiter.api.Test;

class InventoryControllerAuthorizationTest {
    private final InventoryController controller = new InventoryController(mock(InventoryService.class));

    @Test
    void storeUserCannotReadAnotherStoresQuota() {
        assertThatThrownBy(() -> controller.quota("STORE-BJ-001", "AF-CITY-EV", "STORE-SH-001", "SALES"))
            .hasMessageContaining("own store");
    }

    @Test
    void nonAdminCannotInvokeDiagnosticMutations() {
        var request = new InventoryController.ReserveRequest("order-1", "STORE-SH-001", "AF-CITY-EV");
        assertThatThrownBy(() -> controller.reserve(request, "SALES")).hasMessageContaining("ADMIN");
        assertThatThrownBy(() -> controller.allocateVin("order-1", "SALES")).hasMessageContaining("ADMIN");
        assertThatThrownBy(() -> controller.release("order-1", "SALES")).hasMessageContaining("ADMIN");
    }
}
