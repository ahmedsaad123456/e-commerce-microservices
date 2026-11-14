package com.example.order_service.outbox;

import com.example.order_service.kafka.OrderProducer;
import com.example.order_service.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxRepository outbox;
    private final OrderProducer producer;                 // instead of KafkaTemplate

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    @Transactional      // keeps the row locks until the batch is done; the wait below is short (5s max)
    public void publishPending() {
        for (var event : outbox.lockPending(50)) {
            try {
                producer.publish(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);        // wait for the broker's ack, then mark
                event.markPublished();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                event.recordFailure();
                log.warn("Outbox event {} not published yet (attempt {}): {}",
                        event.getId(), event.getAttempts(), e.toString());
                return;     // Kafka is probably down: stop this round, the next tick tries again
            }
        }
    }
}