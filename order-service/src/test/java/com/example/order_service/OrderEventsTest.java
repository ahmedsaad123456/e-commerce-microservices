package com.example.order_service;

import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderLine;
import com.example.order_service.entity.OrderStatus;
import com.example.order_service.entity.PaymentMethod;
import com.example.order_service.outbox.OrderWriter;
import com.example.order_service.outbox.OutboxStatus;
import com.example.order_service.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "outbox.poll-interval-ms=200"           // do not wait a second in a test
})
@Testcontainers
class OrderEventsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    // @ServiceConnection makes Spring Boot point spring.kafka.bootstrap-servers at this container
    @Container @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.2.0");

    @Autowired
    OrderWriter orderWriter;
    @Autowired
    OutboxRepository outbox;

    @Test
    void savedOrder_endsUpInKafka_andTheOutboxRowIsPublished() {
        var order = Order.builder()
                .reference(UUID.randomUUID())
                .customerId("c-1")
                .paymentMethod(PaymentMethod.CASH)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("200.00"))
                .build();
        order.addLine(OrderLine.builder().productId(1).productName("Laptop")
                .unitPrice(new BigDecimal("100.00")).quantity(2).build());

        orderWriter.saveWithEvent(order);


        // the relay marks the row only after Kafka acknowledged the message
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(outbox.findAll()).allMatch(e -> e.getStatus() == OutboxStatus.PUBLISHED));
    }


}
