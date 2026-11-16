package com.example.order_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

// copy of the Payment service's event (every service owns its copy)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentResult(UUID eventId, String orderReference, PaymentStatus status) {}

