package com.example.order_service.entity;

import java.util.UUID;

public record OrderFailed(UUID eventId, String orderReference, String reason) {}