package com.example.payment_service.dto;

import com.example.payment_service.entity.PaymentStatus;

import java.math.BigDecimal;

public record PaymentView(String orderReference, BigDecimal amount, PaymentStatus status, String reason) {}
