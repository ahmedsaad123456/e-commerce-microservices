package com.example.product_service.kafka;

import com.example.product_service.entity.OrderFailed;
import com.example.product_service.service.StockRestorationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderFailedConsumer {

    private final JsonMapper json;
    private final StockRestorationService restoration;

    @KafkaListener(topics = "order-failed-topic")
    public void onOrderFailed(String payload) {
        var failed = json.readValue(payload, OrderFailed.class);
        restoration.restore(UUID.fromString(failed.orderReference()));     // same idempotent method as PaymentFailed
    }
}
