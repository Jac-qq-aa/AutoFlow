package io.autoflow.gateway.security;

import io.jsonwebtoken.Jwts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final DemoUserService userService;
    private final SecretKey secretKey;

    public AuthController(DemoUserService userService, SecretKey secretKey) {
        this.userService = userService;
        this.secretKey = secretKey;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var user = userService.authenticate(request.username(), request.password())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        var now = Instant.now();
        var expiresAt = now.plusSeconds(8 * 60 * 60);
        var token = Jwts.builder()
            .subject(user.username())
            .claim("role", user.role())
            .claim("storeId", user.storeId())
            .claim("displayName", user.displayName())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey)
            .compact();
        return new LoginResponse(token, expiresAt, user.username(), user.role(), user.storeId(), user.displayName());
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    record LoginResponse(String token, Instant expiresAt, String username, String role, String storeId, String displayName) {}
}

