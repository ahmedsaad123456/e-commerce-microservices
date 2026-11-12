package com.example.order_service.dto;

import com.example.order_service.entity.OrderStatus;
import com.example.order_service.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(Long id, UUID reference, String customerId, BigDecimal totalAmount,
                            PaymentMethod paymentMethod, OrderStatus status, List<OrderLineResponse> lines) {
}
