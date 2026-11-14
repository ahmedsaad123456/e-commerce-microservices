package com.example.order_service.kafka;

import com.example.order_service.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderConfirmation(
        UUID eventId,              // identifies THIS event; the consumer uses it to ignore duplicates
        String orderReference,
        String customerId,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        List<Line> lines) {

    public record Line(Integer productId, String productName, BigDecimal unitPrice, int quantity) {}
}