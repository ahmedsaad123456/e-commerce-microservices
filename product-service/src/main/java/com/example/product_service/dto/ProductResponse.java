package com.example.product_service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

// Serializable because it will be cached in Redis
public record ProductResponse(Integer id, String name, String description,
                              BigDecimal price, Integer categoryId, String categoryName) implements Serializable {
}
