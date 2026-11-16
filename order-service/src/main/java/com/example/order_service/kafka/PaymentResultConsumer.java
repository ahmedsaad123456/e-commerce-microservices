package com.example.order_service.kafka;

import com.example.order_service.entity.PaymentResult;
import com.example.order_service.outbox.PaymentResultHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class PaymentResultConsumer {

    private final JsonMapper json;
    private final PaymentResultHandler handler;

    @KafkaListener(topics = "payment-topic")
    public void onPaymentResult(String payload) {
        handler.apply(json.readValue(payload, PaymentResult.class));
    }
}