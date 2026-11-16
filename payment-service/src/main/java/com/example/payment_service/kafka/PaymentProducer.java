package com.example.payment_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    // The payload is the JSON that is already stored in the outbox row.
    // We return the future (instead of waiting here) so the caller decides how long to wait for the broker.
    public CompletableFuture<SendResult<String, String>> publish(String topic, String key, String payload) {
        return kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {                       // failures are logged by the relay, once
                        var meta = result.getRecordMetadata();
                        log.info("Published key {} to {}-{} at offset {}",
                                key, meta.topic(), meta.partition(), meta.offset());
                    }
                });
    }
}