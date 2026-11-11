package com.example.product_service.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @PositiveOrZero Integer availableQuantity,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull Integer categoryId
) {}

