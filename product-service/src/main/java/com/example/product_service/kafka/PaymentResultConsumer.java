package com.example.product_service.kafka;

import com.example.product_service.service.StockRestorationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentResultConsumer {

    private final JsonMapper json;
    private final StockRestorationService restoration;

    @KafkaListener(topics = "payment-topic")
    public void onPaymentResult(String payload) {
        var result = json.readValue(payload, PaymentResult.class);
        if (result.status() == PaymentStatus.FAILED) {                      // SUCCEEDED needs nothing from Product
            restoration.restore(UUID.fromString(result.orderReference()));  // the order reference IS the purchase id
        }
    }
}