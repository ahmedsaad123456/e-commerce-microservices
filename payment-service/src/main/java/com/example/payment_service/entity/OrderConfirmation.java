package com.example.payment_service.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

// copy of the Order service's event: only the fields the Payment service needs
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderConfirmation(UUID eventId, String orderReference, String customerId,
                                BigDecimal totalAmount, String paymentMethod) {}
