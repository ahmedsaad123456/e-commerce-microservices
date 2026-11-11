package com.example.product_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PurchaseRequest(@NotNull UUID purchaseId, @NotEmpty List<@Valid PurchaseItem> items) {
}
