package com.example.order_service.outbox;

import com.example.order_service.kafka.OrderProducer;
import com.example.order_service.rabbitmq.RabbitEmailPublisher;
import com.example.order_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxRepository outbox;
    private final OrderProducer producer;
    private final RabbitEmailPublisher rabbitPublisher;

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        // a destination that failed in THIS round: we skip its remaining events instead of waiting 5s for each,
        // but the other destination keeps working (RabbitMQ down must not block the Kafka events, and vice versa)
        var unavailable = EnumSet.noneOf(OutboxDestination.class);

        for (var event : outbox.lockPending(50)) {
            if (unavailable.contains(event.getDestination())) continue;
            try {
                switch (event.getDestination()) {
                    case KAFKA -> producer.publish(event.getTopic(), event.getAggregateId(), event.getPayload())
                            .get(5, TimeUnit.SECONDS);
                    case RABBITMQ -> rabbitPublisher.publish(event.getTopic(), event.getRoutingKey(),
                            event.getId(), event.getPayload());
                }
                event.markPublished();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                event.recordFailure();
                unavailable.add(event.getDestination());
                log.warn("Outbox event {} ({}) not published yet, attempt {}: {}",
                        event.getId(), event.getDestination(), event.getAttempts(), e.toString());
            }
        }
    }
}