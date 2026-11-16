package com.example.notification_service.rabbitmq;

import com.example.notification_service.entity.NotificationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SendEmailJob(UUID eventId, NotificationType type, String customerId,
                           String orderReference, BigDecimal amount, String detail) {}
