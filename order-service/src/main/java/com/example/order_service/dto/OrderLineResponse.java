package com.example.order_service.dto;

import java.math.BigDecimal;

public record OrderLineResponse(Integer productId, String productName, BigDecimal unitPrice, int quantity) {
}
