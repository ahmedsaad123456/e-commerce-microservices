package com.example.order_service.dto;

import java.util.List;
import java.util.UUID;

public record PurchaseRequest(UUID purchaseId, List<PurchaseItem> items) {
}
