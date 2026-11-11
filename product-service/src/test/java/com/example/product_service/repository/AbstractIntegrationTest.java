package com.example.product_service.repository;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",              // no Config Server in tests
        "eureka.client.enabled=false",                    // no Eureka in tests
        "spring.jpa.hibernate.ddl-auto=validate",         // entities must match the Flyway schema
        "spring.cache.type=none",                         // no Redis in tests
        "spring.datasource.hikari.maximum-pool-size=60"   // enough connections for the 50-thread race test
})
@Testcontainers
public abstract class AbstractIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
}
