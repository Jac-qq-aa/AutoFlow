package io.autoflow.gateway;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdGatewayFilter implements GlobalFilter, Ordered {
    private static final String HEADER = "X-Trace-Id";
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        var traceId = incoming == null || incoming.isBlank() ? UUID.randomUUID().toString().replace("-", "") : incoming;
        var mutated = exchange.mutate().request(request -> request.headers(headers -> headers.set(HEADER, traceId))).build();
        mutated.getResponse().getHeaders().set(HEADER, traceId);
        return chain.filter(mutated);
    }
    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}

