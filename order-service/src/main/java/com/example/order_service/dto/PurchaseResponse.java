package com.example.order_service.dto;

import java.math.BigDecimal;

public record PurchaseResponse(Integer productId, String name, BigDecimal unitPrice, int quantity) {
}
