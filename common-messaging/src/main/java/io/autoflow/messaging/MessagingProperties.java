package io.autoflow.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "autoflow.messaging")
public record MessagingProperties(boolean enabled, String endpoints, int maxDeliveryAttempts) {
    public MessagingProperties {
        endpoints = endpoints == null || endpoints.isBlank() ? "localhost:8081" : endpoints;
        maxDeliveryAttempts = maxDeliveryAttempts <= 0 ? 16 : maxDeliveryAttempts;
    }
}
