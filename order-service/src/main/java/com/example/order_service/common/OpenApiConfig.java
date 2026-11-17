package com.example.order_service.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderOpenAPI(
            @Value("${spring.application.name}") String serviceName) {

        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .version("v1"))
                // Use the current browser origin, so Try it out goes through Nginx/Gateway.
                .addServersItem(new Server().url("/"));
    }
}