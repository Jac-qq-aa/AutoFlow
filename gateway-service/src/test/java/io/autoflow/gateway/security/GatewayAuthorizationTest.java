package io.autoflow.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false"
})
@AutoConfigureWebTestClient
class GatewayAuthorizationTest {
    @Autowired WebTestClient client;

    @Test
    void salesCannotReadOperationalDeadLetters() {
        getAs("/api/events/order/dead-letters", "SALES").expectStatus().isForbidden();
    }

    @Test
    void salesCannotCallInventoryMutationDiagnostics() {
        postAs("/api/inventory/reservations", "SALES").expectStatus().isForbidden();
        postAs("/api/inventory/reservations/an-order/allocate-vin", "SALES").expectStatus().isForbidden();
        postAs("/api/inventory/reservations/an-order/release", "SALES").expectStatus().isForbidden();
    }

    @Test
    void salesCannotForgeFulfillmentEventsOrCompleteDelivery() {
        postAs("/api/fulfillment/payments", "SALES").expectStatus().isForbidden();
        postAs("/api/fulfillment/refunds", "SALES").expectStatus().isForbidden();
        postAs("/api/fulfillment/deliveries", "SALES").expectStatus().isForbidden();
        postAs("/api/fulfillment/deliveries/an-order/complete", "SALES").expectStatus().isForbidden();
    }

    @Test
    void adminAndDeliveryRolesReachTheirAuthorizedBoundary() {
        getAs("/api/events/order/dead-letters", "ADMIN").expectStatus().value(status -> assertThat(status).isNotEqualTo(403));
        postAs("/api/inventory/reservations", "ADMIN").expectStatus().value(status -> assertThat(status).isNotEqualTo(403));
        postAs("/api/fulfillment/deliveries/an-order/complete", "DELIVERY").expectStatus().value(status -> assertThat(status).isNotEqualTo(403));
    }

    private WebTestClient.ResponseSpec getAs(String uri, String role) {
        return client.mutateWith(mockJwt().authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role))))
            .get().uri(uri).exchange();
    }

    private WebTestClient.ResponseSpec postAs(String uri, String role) {
        return client.mutateWith(mockJwt().authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role))))
            .post().uri(uri).exchange();
    }
}
