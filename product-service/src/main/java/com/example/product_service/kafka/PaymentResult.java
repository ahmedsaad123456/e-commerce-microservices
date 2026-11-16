package com.example.product_service.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

// copy of the Payment service's event: Product only needs these fields
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentResult(UUID eventId, String orderReference, PaymentStatus status) {
}
