package com.example.order_service.customer;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class CustomerFeignSecurityConfig {

    @Bean
    RequestInterceptor userTokenRelayInterceptor() {

        return template -> {

            Authentication auth =
                    SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof JwtAuthenticationToken jwtAuth) {

                template.removeHeader(HttpHeaders.AUTHORIZATION);

                template.header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + jwtAuth.getToken().getTokenValue()
                );
            }
        };
    }
}