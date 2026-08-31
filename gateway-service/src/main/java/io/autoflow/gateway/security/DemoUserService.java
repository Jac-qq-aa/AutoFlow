package io.autoflow.gateway.security;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DemoUserService {
    private final Map<String, DemoUser> users = List.of(
        new DemoUser("sales", "demo123", "SALES", "STORE-SH-001", "上海门店销售"),
        new DemoUser("manager", "demo123", "STORE_MANAGER", "STORE-SH-001", "上海门店经理"),
        new DemoUser("delivery", "demo123", "DELIVERY", "STORE-SH-001", "交付专员"),
        new DemoUser("admin", "demo123", "ADMIN", "*", "平台管理员")
    ).stream().collect(Collectors.toUnmodifiableMap(DemoUser::username, Function.identity()));

    public Optional<DemoUser> authenticate(String username, String password) {
        return Optional.ofNullable(users.get(username)).filter(user -> user.password().equals(password));
    }
}

