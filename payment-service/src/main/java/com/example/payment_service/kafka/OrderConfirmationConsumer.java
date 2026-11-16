package com.example.payment_service.kafka;

import com.example.payment_service.entity.OrderConfirmation;
import com.example.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmationConsumer {

    private final JsonMapper json;
    private final PaymentService paymentService;

    // We receive the raw JSON (String) and parse it ourselves: a corrupt message then becomes an ordinary
    // exception inside our method, which the error handler below can retry or send to a dead-letter topic.
    @KafkaListener(topics = "order-topic")
    public void onOrder(String payload) {
        paymentService.process(json.readValue(payload, OrderConfirmation.class));
    }
}
