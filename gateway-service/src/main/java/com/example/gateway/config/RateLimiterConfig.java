package com.example.gateway.config;


import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> {
            // Behind Nginx the real client IP arrives in X-Forwarded-For.
            // Nginx OVERWRITES this header, so a client cannot spoof it through Nginx.
            // If clients can reach the gateway directly, they could fake it to dodge the limit.


            // Read the original client IP forwarded by the trusted reverse proxy (Nginx).
            String forwarded = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");

            if (forwarded != null && !forwarded.isBlank()) {

                // The first IP represents the original client IP in the forwarded chain.
                return Mono.just(forwarded.split(",")[0].trim());
            }

            // Fall back to the direct remote address if no forwarded IP is available.
            var remote = exchange.getRequest().getRemoteAddress();

            // Use "unknown" if the remote address cannot be determined.
            return Mono.just(
                    remote != null
                            ? remote.getAddress().getHostAddress()
                            : "unknown"
            );
        };
    }
}