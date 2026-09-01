package io.autoflow.order.application;

import io.autoflow.common.error.AccessDeniedException;
import java.util.Arrays;

public record RequestUser(String userId, String role, String storeId) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public void requireAnyRole(String... accepted) {
        if (Arrays.stream(accepted).noneMatch(role::equals)) {
            throw new AccessDeniedException("ROLE_ACCESS_DENIED", "Role " + role + " cannot perform this operation");
        }
    }
}
