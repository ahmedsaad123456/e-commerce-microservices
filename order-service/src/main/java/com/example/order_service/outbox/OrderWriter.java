package com.example.order_service.outbox;

import com.example.order_service.common.Constants;
import com.example.order_service.entity.Order;
import com.example.order_service.kafka.KafkaTopicConfig;
import com.example.order_service.mapper.OrderEventMapper;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderWriter {

    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final JsonMapper json;
    private final OrderEventMapper orderEventMapper;

    @Transactional
    public Order saveWithEvent(Order order) {
        var saved = orders.save(order);

        var eventId = UUID.randomUUID();       // generated ONCE; the consumers deduplicate on it
        var payload = json.writeValueAsString(orderEventMapper.toOrderConfirmation(saved, eventId));
        outbox.save(OutboxEvent.pending(eventId, Constants.ORDER, saved.getReference().toString(),
                KafkaTopicConfig.ORDER_TOPIC, payload));

        return saved;                           // both rows commit together, or neither does
    }
}