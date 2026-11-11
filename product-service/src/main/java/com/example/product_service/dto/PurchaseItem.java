package com.example.product_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseItem(@NotNull Integer productId, @NotNull @Positive Integer quantity) {
}
