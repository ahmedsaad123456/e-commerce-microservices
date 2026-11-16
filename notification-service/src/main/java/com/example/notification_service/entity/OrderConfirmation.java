package com.example.notification_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderConfirmation(UUID eventId, String orderReference, String customerId, BigDecimal totalAmount) {}

