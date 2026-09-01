package io.autoflow.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/api/auth/login", "/actuator/**").permitAll()
                .pathMatchers("/api/events/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/inventory/reservations").hasRole("ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/inventory/reservations/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/fulfillment/deliveries/*/complete").hasAnyRole("DELIVERY", "ADMIN")
                .pathMatchers("/api/fulfillment/**").denyAll()
                .pathMatchers(HttpMethod.POST, "/api/orders/*/approve").hasAnyRole("STORE_MANAGER", "ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole("SALES", "STORE_MANAGER", "ADMIN")
                .anyExchange().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .build();
    }

    private ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var role = jwt.getClaimAsString("role");
            return role == null ? Flux.empty() : Flux.just(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }
}
