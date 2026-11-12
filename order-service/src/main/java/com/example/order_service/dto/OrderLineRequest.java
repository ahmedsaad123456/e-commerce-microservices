package com.example.order_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequest(@NotNull Integer productId, @NotNull @Positive Integer quantity) {
}
