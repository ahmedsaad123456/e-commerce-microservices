package com.example.order_service.outbox;

import com.example.order_service.common.Constants;
import com.example.order_service.entity.Order;
import com.example.order_service.kafka.KafkaTopicConfig;
import com.example.order_service.mapper.OrderEventMapper;
import com.example.order_service.rabbitmq.NotificationType;
import com.example.order_service.rabbitmq.RabbitTopology;
import com.example.order_service.rabbitmq.SendEmailJob;
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
        var reference = saved.getReference().toString();

        // 1) the event for the Payment service (Kafka)
        var eventId = UUID.randomUUID();
        var payload = json.writeValueAsString(orderEventMapper.toOrderConfirmation(saved, eventId));
        outbox.save(OutboxEvent.pending(eventId, Constants.ORDER, reference, KafkaTopicConfig.ORDER_TOPIC, payload));

        // 2) the email request for the Notification service (RabbitMQ). Same transaction: both rows or none
        var emailId = UUID.randomUUID();
        var email = new SendEmailJob(emailId, NotificationType.ORDER_RECEIVED,
                saved.getCustomerId(), reference, saved.getTotalAmount(), null);
        outbox.save(OutboxEvent.pendingRabbit(emailId, Constants.ORDER, reference,
                RabbitTopology.EMAIL_EXCHANGE, RabbitTopology.KEY_ORDER, json.writeValueAsString(email)));

        return saved;
    }
}