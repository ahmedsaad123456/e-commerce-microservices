package com.example.order_service.outbox;

import com.example.order_service.common.Constants;
import com.example.order_service.entity.OrderFailed;
import com.example.order_service.kafka.KafkaTopicConfig;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderFailureWriter {

    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final JsonMapper json;

    // Its own transaction: the one of the failed save already rolled back, and took its outbox rows with it
    @Transactional
    public void recordFailure(UUID reference, RuntimeException cause) {
        // The exception can be misleading: the commit may have succeeded and only the reply was lost
        // (or a mapper failed AFTER the save). If the order exists it is real: its stock must stay deducted.
        if (orders.existsByReference(reference)) {
            return;
        }
        var eventId = UUID.randomUUID();
        var event = new OrderFailed(eventId, reference.toString(), cause.getClass().getSimpleName());
        outbox.save(OutboxEvent.pending(eventId, Constants.ORDER, reference.toString(),
                KafkaTopicConfig.ORDER_FAILED_TOPIC, json.writeValueAsString(event)));
    }
}