package com.example.payment_service.entity;


import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResult(UUID eventId, String orderReference, String customerId,
                            BigDecimal amount, PaymentStatus status, String reason) {}