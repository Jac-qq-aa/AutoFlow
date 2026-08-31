package io.autoflow.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserContextHeaderFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
            .filter(JwtAuthenticationToken.class::isInstance)
            .cast(JwtAuthenticationToken.class)
            .map(authentication -> {
                var jwt = authentication.getToken();
                return exchange.mutate().request(request -> request.headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                    headers.remove("X-Store-Id");
                    headers.add("X-User-Id", jwt.getSubject());
                    headers.add("X-User-Role", jwt.getClaimAsString("role"));
                    headers.add("X-Store-Id", jwt.getClaimAsString("storeId"));
                })).build();
            })
            .defaultIfEmpty(exchange)
            .flatMap(mutated -> chain.filter(mutated));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
