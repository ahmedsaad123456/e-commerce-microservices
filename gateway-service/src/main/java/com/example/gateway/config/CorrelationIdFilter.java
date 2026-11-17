package com.example.gateway.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    // Accept a client-supplied id only if it is short and safe.
    // Otherwise a client could inject huge values or newlines (log injection) into every log line.
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    // Identifies which gateway instance answered (used in step 11).
    @Value("${GATEWAY_NAME:${spring.application.name}}")
    private String gatewayInstance;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = (incoming != null && VALID.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        // The incoming request is immutable, so we build a mutated copy that carries the header downstream.
        ServerHttpRequest request = exchange.getRequest().mutate().header(HEADER, id).build();

        // Echo it to the caller so a user can quote it when reporting a problem.
        exchange.getResponse().getHeaders().set(HEADER, id);
        exchange.getResponse()
                .getHeaders()
                .set("X-Gateway-Instance", gatewayInstance);
        return chain.filter(exchange.mutate().request(request).build());
    }

    // Run first, so every other filter (rate limiter, circuit breaker) already sees the id.
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
