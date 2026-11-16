package com.example.notification_service.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("notifications")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @Indexed(unique = true)            // ONE notification per Kafka event: a redelivered event cannot create a second one
    private String eventId;

    private NotificationType type;
    private String customerId;
    private String failureReason;

    private String orderReference;
    private BigDecimal amount;
    private String detail;             // e.g. the decline reason
    private NotificationStatus status;
    private Instant createdAt;
    private Instant sentAt;

    public static Notification queued(String eventId, NotificationType type, String customerId,
                                      String orderReference, BigDecimal amount, String detail) {
        return Notification.builder().eventId(eventId).type(type).customerId(customerId)
                .orderReference(orderReference).amount(amount).detail(detail)
                .status(NotificationStatus.QUEUED).createdAt(Instant.now()).build();
    }

    public void markSent() { this.status = NotificationStatus.SENT; this.sentAt = Instant.now(); }

    public void markFailed(String reason) { this.status = NotificationStatus.FAILED; this.failureReason = reason; }
}

