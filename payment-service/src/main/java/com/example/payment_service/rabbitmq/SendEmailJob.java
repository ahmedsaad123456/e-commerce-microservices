package com.example.payment_service.rabbitmq;

import java.math.BigDecimal;
import java.util.UUID;

// everything the Notification service needs to send one email (no customer data: it looks the email up itself)
public record SendEmailJob(UUID eventId, NotificationType type, String customerId,
                           String orderReference, BigDecimal amount, String detail) {
}
