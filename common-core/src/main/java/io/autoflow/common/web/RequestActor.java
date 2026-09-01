package io.autoflow.common.web;

import io.autoflow.common.error.AccessDeniedException;
import java.util.Arrays;

public record RequestActor(String userId, String role, String storeId) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public void requireAnyRole(String... allowedRoles) {
        if (Arrays.stream(allowedRoles).noneMatch(role::equals)) {
            throw new AccessDeniedException("ROLE_ACCESS_DENIED", "This operation is restricted to " + String.join(", ", allowedRoles));
        }
    }

    public void requireStore(String requestedStoreId) {
        if (!isAdmin() && !storeId.equals(requestedStoreId)) {
            throw new AccessDeniedException("STORE_ACCESS_DENIED", "Users can only access their own store");
        }
    }
}
