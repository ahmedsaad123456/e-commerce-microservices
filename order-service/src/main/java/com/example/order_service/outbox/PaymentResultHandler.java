package com.example.order_service.outbox;

import com.example.order_service.entity.PaymentResult;
import com.example.order_service.entity.PaymentStatus;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.ProcessedEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentResultHandler {

    private final ProcessedEvents processedEvents;
    private final OrderRepository orders;

    @Transactional
    public void apply(PaymentResult result) {
        // Same transaction as the status change: if anything below fails, this marker rolls back too,
        // so the retry is NOT mistaken for a duplicate.
        if (!processedEvents.markProcessed(result.eventId())) {
            log.info("Duplicate event {} ignored", result.eventId());
            return;
        }

        var order = orders.findByReference(UUID.fromString(result.orderReference()))
                .orElseThrow(() -> new IllegalStateException("No order for reference " + result.orderReference()));

        boolean changed = result.status() == PaymentStatus.SUCCEEDED ? order.confirm() : order.cancel();
        log.info("Order {} payment {} -> status {}{}", order.getReference(), result.status(), order.getStatus(),
                changed ? "" : " (unchanged: it was not PENDING)");
    }
}