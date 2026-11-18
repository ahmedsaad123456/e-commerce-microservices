package com.example.gateway.security;


import com.example.gateway.handler.GatewaySecurityErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final GatewaySecurityErrorHandler errorHandler;

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                // CSRF protects cookie-based sessions. We use a Bearer header and no cookies, so it is not needed.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // CORS lives HERE (explained right below). It must run before authentication.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeExchange(auth -> auth
                        // Docker healthcheck and Prometheus (nginx will hide /actuator from the outside, step below)
                        .pathMatchers("/actuator/health/**", "/actuator/prometheus").permitAll()
                        // Swagger UI and the docs routes from the previous part
                        // Swagger UI and the docs routes
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**",
                                        "/docs/**", "/v3/api-docs/**").permitAll()                        // Internal endpoint: only order-service calls it (directly through Eureka, NOT through the gateway)
                        .pathMatchers("/api/v1/products/purchase").denyAll()
                        .anyExchange().authenticated()
                )

                // Turns on token validation: signature, expiry, issuer.
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(errorHandler)   // bad/missing token on a protected route
                        .accessDeniedHandler(errorHandler))

                // No token at all (anonymous user) goes through this one.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));

        return http.build();
    }

    // The single CORS configuration of the whole system.
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:4200"));   // explicit list, never "*" with real users
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));   // so browser JS can read it from responses
        config.setAllowCredentials(false);   // we send the token in a header, not in cookies
        config.setMaxAge(3600L);             // the browser may cache the preflight answer for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}