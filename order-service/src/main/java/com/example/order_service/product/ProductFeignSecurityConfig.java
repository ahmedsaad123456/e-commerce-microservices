package com.example.order_service.product;


import com.example.order_service.config.OrderServiceTokenProvider;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

public class ProductFeignSecurityConfig {

    @Bean
    RequestInterceptor serviceTokenInterceptor(
            OrderServiceTokenProvider tokens
    ) {

        return template -> {

            // Replace any existing Authorization header.
            template.removeHeader(HttpHeaders.AUTHORIZATION);

            // Attach the Order Service access token.
            template.header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + tokens.getToken()
            );
        };
    }
}